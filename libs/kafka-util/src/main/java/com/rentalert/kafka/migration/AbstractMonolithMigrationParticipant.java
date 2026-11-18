package com.rentalert.kafka.migration;

import com.rentalert.dto.migration.TableResult;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public abstract class AbstractMonolithMigrationParticipant extends AbstractMigrationParticipant {

    private static final String DISABLED_MESSAGE = "migration is disabled: set app.migration.enabled=true";
    private static final int PREVIEW_IDS = 20;

    @Autowired
    private JdbcTemplate target;

    @Autowired
    private ObjectProvider<MonolithSource> monolithSource;

    @Autowired
    private TransactionTemplate targetTransactions;

    protected AbstractMonolithMigrationParticipant(String serviceName) {
        super(serviceName);
    }

    protected abstract List<MigrationTable> tables();

    protected void preflight(MigrationContext context, JdbcTemplate source) {
    }

    protected List<TableResult> runTable(MigrationTable table, MigrationContext context) {
        return table.toMigrator(source(), target, targetTransactions).migrate(context)
            .map(List::of)
            .orElse(List.of());
    }

    @Override
    protected final List<TableResult> migrate(MigrationContext context) {
        JdbcTemplate source = source();
        preflight(context, source);
        List<TableResult> results = new ArrayList<>();
        for (MigrationTable table : tables()) {
            results.addAll(runTable(table, context));
        }
        return results;
    }

    protected final long countSource(String sql, Object... args) {
        Long count = source().queryForObject(sql, Long.class, args);
        return count == null ? 0 : count;
    }

    protected final List<Long> idsSource(String sql, Object... args) {
        return source().query(sql, (rs, rowNum) -> rs.getLong(1), args);
    }

    protected final void requireNone(String label, String sql, Object... args) {
        List<Long> ids = idsSource(sql, args);
        if (!ids.isEmpty()) {
            throw new MigrationPreflightException(label + ": " + ids.size()
                + " row(s) violate the precondition; ids " + ids.subList(0, Math.min(PREVIEW_IDS, ids.size())));
        }
    }

    protected final void warnIfAny(String label, String sql, Object... args) {
        long rows = countSource(sql, args);
        if (rows > 0) {
            log.warn("[MIGRATION] {}: {} row(s)", label, rows);
        }
    }

    private JdbcTemplate source() {
        MonolithSource source = monolithSource.getIfAvailable();
        if (source == null) {
            throw new IllegalStateException(DISABLED_MESSAGE);
        }
        return source.jdbcTemplate();
    }
}
