package com.rentalert.kafka.migration;

import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Builder
public class KeysetTableMigrator {

    private static final String SINCE_PLACEHOLDER = "{since}";
    private static final String AFTER_PLACEHOLDER = "{after}";

    private final String table;
    private final JdbcTemplate source;
    private final String sourceSelectSql;
    private final String sourceCountSql;
    private final String sinceColumn;
    private final String keyColumn;
    private final List<String> keyColumns;
    @Builder.Default
    private final Object startAfter = 0L;
    private final List<Object> startAfterValues;
    private final RowMapper<Object[]> rowMapper;
    private final JdbcTemplate target;
    private final TransactionTemplate targetTransactions;
    private final String targetInsertSql;
    private final String sequenceTable;
    private final String sequenceColumn;

    public Optional<TableResult> migrate(MigrationContext context) {
        MigrationSettings settings = context.settings();
        if (!isSelected(settings)) {
            return Optional.empty();
        }
        boolean dryRun = settings.dryRun();
        Instant since = settings.since();
        int batchSize = batchSize(settings.batchSize());
        Long limit = settings.limitPerTable();
        long total = count(since);

        long processed = 0;
        long migrated = 0;
        long skipped = 0;
        long failed = 0;
        Object[] cursor = initialCursor();
        while (limit == null || processed < limit) {
            if (context.cancelled()) {
                throw new IllegalStateException("Migration of " + table + " was cancelled");
            }
            int want = limit == null ? batchSize : (int) Math.min(batchSize, limit - processed);
            Batch batch = fetch(cursor, want, since);
            if (batch.fetched == 0) {
                break;
            }
            cursor = batch.cursor;
            processed += batch.fetched;
            failed += batch.failed;
            if (!dryRun && !batch.rows.isEmpty()) {
                int inserted = write(batch.rows);
                migrated += inserted;
                skipped += batch.rows.size() - inserted;
            }
            context.progress(table, processed, total);
            if (batch.fetched < want) {
                break;
            }
        }
        context.finishTable(table, processed, total);
        if (!dryRun && sequenceColumn != null) {
            resetSequence();
        }
        log.info("[MIGRATION] {}: total={} migrated={} skipped={} failed={}", table, total, migrated, skipped, failed);
        return Optional.of(new TableResult(table, total, migrated, skipped, failed));
    }

    private boolean isSelected(MigrationSettings settings) {
        List<String> tables = settings.tables();
        return tables == null || tables.isEmpty() || tables.contains(table);
    }

    private static int batchSize(int requested) {
        return requested <= 0
            ? MigrationSettings.DEFAULT_BATCH_SIZE
            : Math.min(requested, MigrationSettings.MAX_BATCH_SIZE);
    }

    private long count(Instant since) {
        Object[] args = sinceApplies(since) ? new Object[] {Timestamp.from(since)} : new Object[0];
        Long total = source.queryForObject(withSince(sourceCountSql, since), Long.class, args);
        return total == null ? 0 : total;
    }

    private Object[] initialCursor() {
        if (keyColumns == null) {
            return new Object[] {startAfter};
        }
        if (startAfterValues != null) {
            return startAfterValues.toArray();
        }
        if (!sourceSelectSql.contains(AFTER_PLACEHOLDER)) {
            throw new IllegalStateException(
                "Table " + table + " has a composite key without startAfterValues and without " + AFTER_PLACEHOLDER);
        }
        return null;
    }

    private Batch fetch(Object[] cursor, int limit, Instant since) {
        List<Object> args = new ArrayList<>();
        if (cursor != null) {
            args.addAll(Arrays.asList(cursor));
        }
        if (sinceApplies(since)) {
            args.add(Timestamp.from(since));
        }
        args.add(limit);
        Batch batch = new Batch(cursor);
        source.query(withSince(withAfter(sourceSelectSql, cursor), since), args.toArray(), (RowCallbackHandler) rs -> {
            batch.cursor = keyValues(rs);
            batch.fetched++;
            try {
                batch.rows.add(rowMapper.mapRow(rs, batch.fetched));
            } catch (SQLException | RuntimeException e) {
                batch.failed++;
                log.warn("[MIGRATION] {}: row {} could not be converted: {}", table, Arrays.toString(batch.cursor), e.getMessage());
            }
        });
        return batch;
    }

    private Object[] keyValues(ResultSet rs) throws SQLException {
        if (keyColumns == null) {
            return new Object[] {rs.getObject(keyColumn)};
        }
        Object[] values = new Object[keyColumns.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = rs.getObject(keyColumns.get(i));
        }
        return values;
    }

    private String withAfter(String sql, Object[] cursor) {
        if (keyColumns == null || !sql.contains(AFTER_PLACEHOLDER)) {
            return sql;
        }
        if (cursor == null) {
            return sql.replace(AFTER_PLACEHOLDER, "");
        }
        String placeholders = String.join(", ", Collections.nCopies(keyColumns.size(), "?"));
        return sql.replace(AFTER_PLACEHOLDER, "AND (" + String.join(", ", keyColumns) + ") > (" + placeholders + ")");
    }

    private int write(List<Object[]> rows) {
        return targetTransactions.execute(status -> Arrays.stream(target.batchUpdate(targetInsertSql, rows)).sum());
    }

    private void resetSequence() {
        String sql = String.format(
            "SELECT setval(pg_get_serial_sequence('%1$s', '%2$s'), COALESCE(MAX(%2$s), 0) + 1, false) FROM %1$s",
            sequenceTable, sequenceColumn);
        targetTransactions.executeWithoutResult(status -> target.execute(sql));
    }

    private String withSince(String sql, Instant since) {
        if (!sinceApplies(since)) {
            return sql.replace(SINCE_PLACEHOLDER, "");
        }
        return sql.replace(SINCE_PLACEHOLDER, "AND " + sinceColumn + " >= ?");
    }

    private boolean sinceApplies(Instant since) {
        return since != null && sinceColumn != null;
    }

    private static final class Batch {

        private Object[] cursor;
        private int fetched;
        private int failed;
        private final List<Object[]> rows = new ArrayList<>();

        private Batch(Object[] cursor) {
            this.cursor = cursor;
        }
    }
}
