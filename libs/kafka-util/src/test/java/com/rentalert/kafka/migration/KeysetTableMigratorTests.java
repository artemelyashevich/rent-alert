package com.rentalert.kafka.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class KeysetTableMigratorTests {

    private static final Instant OLD = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NEW = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant SINCE = Instant.parse("2026-03-01T00:00:00Z");
    private static final String FAVORITE_SQL_AFTER =
        "SELECT user_id, listing_id, created_at FROM favorite_src WHERE 1 = 1 {after} {since} ORDER BY user_id, listing_id LIMIT ?";
    private static final String FAVORITE_SQL_ROW =
        "SELECT user_id, listing_id, created_at FROM favorite_src WHERE (user_id, listing_id) > (?, ?) {since} ORDER BY user_id, listing_id LIMIT ?";
    private static final List<String> ALL_FAVORITE_KEYS = List.of("1:a", "1:b", "1:c", "2:a", "2:b", "3:a", "3:z");

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactions;
    private MigrationControl control;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:migration-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        jdbcTemplate.execute("CREATE TABLE listing_src (id BIGINT PRIMARY KEY, name VARCHAR(100), created_at TIMESTAMP)");
        jdbcTemplate.execute("CREATE TABLE listing (id BIGINT PRIMARY KEY, name VARCHAR(100))");
        jdbcTemplate.execute("CREATE TABLE favorite_src (user_id BIGINT, listing_id VARCHAR(50), created_at TIMESTAMP, PRIMARY KEY (user_id, listing_id))");
        jdbcTemplate.execute("CREATE TABLE favorite (user_id BIGINT, listing_id VARCHAR(50), PRIMARY KEY (user_id, listing_id))");
        control = mock(MigrationControl.class);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SHUTDOWN");
    }

    @Test
    void transfersAllRowsInBatches() {
        insertSource(1, 7, OLD);

        TableResult result = run(settings(3, null, null, false, List.of())).orElseThrow();

        assertThat(result).isEqualTo(new TableResult("listing", 7, 7, 0, 0));
        assertThat(targetRowCount()).isEqualTo(7);
    }

    @Test
    void limitPerTableStopsAfterLimit() {
        insertSource(1, 7, OLD);

        TableResult result = run(settings(3, 4L, null, false, List.of())).orElseThrow();

        assertThat(result).isEqualTo(new TableResult("listing", 7, 4, 0, 0));
        assertThat(targetRowCount()).isEqualTo(4);
    }

    @Test
    void dryRunReadsButWritesNothing() {
        insertSource(1, 7, OLD);

        TableResult result = run(settings(3, null, null, true, List.of())).orElseThrow();

        assertThat(result).isEqualTo(new TableResult("listing", 7, 0, 0, 0));
        assertThat(targetRowCount()).isZero();
    }

    @Test
    void sinceFilterSelectsOnlyRowsNewerThanSince() {
        insertSource(1, 3, OLD);
        insertSource(4, 4, NEW);

        TableResult result = run(settings(10, null, SINCE, false, List.of())).orElseThrow();

        assertThat(result).isEqualTo(new TableResult("listing", 4, 4, 0, 0));
        assertThat(jdbcTemplate.queryForList("SELECT id FROM listing ORDER BY id", Long.class))
            .containsExactly(4L, 5L, 6L, 7L);
    }

    @Test
    void cancellationBetweenBatchesStopsMigration() {
        insertSource(1, 7, OLD);
        AtomicInteger checks = new AtomicInteger();
        when(control.isCancelled(any())).thenAnswer(invocation -> checks.incrementAndGet() > 1);

        assertThatThrownBy(() -> run(settings(3, null, null, false, List.of())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cancelled");

        assertThat(targetRowCount()).isEqualTo(3);
    }

    @Test
    void rerunIsIdempotent() {
        insertSource(1, 5, OLD);
        run(settings(2, null, null, false, List.of()));

        TableResult second = run(settings(2, null, null, false, List.of())).orElseThrow();

        assertThat(second).isEqualTo(new TableResult("listing", 5, 0, 5, 0));
        assertThat(targetRowCount()).isEqualTo(5);
    }

    @Test
    void rowThatCannotBeConvertedIsCountedAsFailed() {
        insertSource(1, 2, OLD);
        jdbcTemplate.update("INSERT INTO listing_src (id, name, created_at) VALUES (3, 'bad', ?)", Timestamp.from(OLD));
        insertSource(4, 2, OLD);

        TableResult result = run(settings(10, null, null, false, List.of())).orElseThrow();

        assertThat(result).isEqualTo(new TableResult("listing", 5, 4, 0, 1));
        assertThat(targetRowCount()).isEqualTo(4);
    }

    @Test
    void tableOutsideSettingsFilterIsSkipped() {
        insertSource(1, 3, OLD);

        Optional<TableResult> result = run(settings(10, null, null, false, List.of("other")));

        assertThat(result).isEmpty();
        assertThat(targetRowCount()).isZero();
    }

    @Test
    void compositeKeyTransfersAllRowsInBatches() {
        insertFavoriteSource(OLD);

        TableResult result = runFavorites(settings(3, null, null, false, List.of()), FAVORITE_SQL_AFTER, null).orElseThrow();

        assertThat(result).isEqualTo(new TableResult("favorite", 7, 7, 0, 0));
        assertThat(favoriteKeys()).isEqualTo(ALL_FAVORITE_KEYS);
    }

    @Test
    void compositeKeyLimitPerTableStopsAfterLimit() {
        insertFavoriteSource(OLD);

        TableResult result = runFavorites(settings(3, 4L, null, false, List.of()), FAVORITE_SQL_AFTER, null).orElseThrow();

        assertThat(result).isEqualTo(new TableResult("favorite", 7, 4, 0, 0));
        assertThat(favoriteKeys()).containsExactly("1:a", "1:b", "1:c", "2:a");
    }

    @Test
    void compositeKeySinceSelectsOnlyRowsNewerThanSince() {
        insertFavorite(1, "a", OLD);
        insertFavorite(1, "b", OLD);
        insertFavorite(1, "c", OLD);
        insertFavorite(2, "a", NEW);
        insertFavorite(2, "b", NEW);
        insertFavorite(3, "a", NEW);
        insertFavorite(3, "z", NEW);

        TableResult result = runFavorites(settings(10, null, SINCE, false, List.of()), FAVORITE_SQL_AFTER, null).orElseThrow();

        assertThat(result).isEqualTo(new TableResult("favorite", 4, 4, 0, 0));
        assertThat(favoriteKeys()).containsExactly("2:a", "2:b", "3:a", "3:z");
    }

    @Test
    void compositeKeyResumeAfterFirstBatchDoesNotLoseOrDuplicateRows() {
        insertFavoriteSource(OLD);

        TableResult first = runFavorites(settings(10, 2L, null, false, List.of()), FAVORITE_SQL_AFTER, null).orElseThrow();
        TableResult second = runFavorites(settings(10, null, null, false, List.of()), FAVORITE_SQL_ROW, List.of(1L, "b")).orElseThrow();

        assertThat(first).isEqualTo(new TableResult("favorite", 7, 2, 0, 0));
        assertThat(second).isEqualTo(new TableResult("favorite", 7, 5, 0, 0));
        assertThat(favoriteKeys()).isEqualTo(ALL_FAVORITE_KEYS);
    }

    @Test
    void compositeKeyRerunIsIdempotent() {
        insertFavoriteSource(OLD);
        runFavorites(settings(2, null, null, false, List.of()), FAVORITE_SQL_AFTER, null);

        TableResult second = runFavorites(settings(2, null, null, false, List.of()), FAVORITE_SQL_AFTER, null).orElseThrow();

        assertThat(second).isEqualTo(new TableResult("favorite", 7, 0, 7, 0));
        assertThat(favoriteKeys()).isEqualTo(ALL_FAVORITE_KEYS);
    }

    private Optional<TableResult> runFavorites(MigrationSettings settings, String selectSql, List<Object> startAfterValues) {
        MigrationContext context = new MigrationContext(UUID.randomUUID(), 1, "favorite-ms", settings, control, progress -> { });
        return compositeMigrator(selectSql, startAfterValues).migrate(context);
    }

    private KeysetTableMigrator compositeMigrator(String selectSql, List<Object> startAfterValues) {
        RowMapper<Object[]> mapper = (rs, rowNum) -> new Object[] {rs.getLong("user_id"), rs.getString("listing_id")};
        return KeysetTableMigrator.builder()
            .table("favorite")
            .source(jdbcTemplate)
            .sourceSelectSql(selectSql)
            .sourceCountSql("SELECT count(*) FROM favorite_src WHERE 1 = 1 {since}")
            .sinceColumn("created_at")
            .keyColumns(List.of("user_id", "listing_id"))
            .startAfterValues(startAfterValues)
            .rowMapper(mapper)
            .target(jdbcTemplate)
            .targetTransactions(transactions)
            .targetInsertSql("INSERT INTO favorite (user_id, listing_id) VALUES (?, ?) ON CONFLICT DO NOTHING")
            .build();
    }

    private void insertFavoriteSource(Instant createdAt) {
        insertFavorite(1, "a", createdAt);
        insertFavorite(1, "b", createdAt);
        insertFavorite(1, "c", createdAt);
        insertFavorite(2, "a", createdAt);
        insertFavorite(2, "b", createdAt);
        insertFavorite(3, "a", createdAt);
        insertFavorite(3, "z", createdAt);
    }

    private void insertFavorite(long userId, String listingId, Instant createdAt) {
        jdbcTemplate.update("INSERT INTO favorite_src (user_id, listing_id, created_at) VALUES (?, ?, ?)",
            userId, listingId, Timestamp.from(createdAt));
    }

    private List<String> favoriteKeys() {
        return jdbcTemplate.queryForList(
            "SELECT user_id || ':' || listing_id FROM favorite ORDER BY user_id, listing_id", String.class);
    }

    private Optional<TableResult> run(MigrationSettings settings) {
        MigrationContext context = new MigrationContext(UUID.randomUUID(), 1, "listing-ms", settings, control, progress -> { });
        return migrator().migrate(context);
    }

    private KeysetTableMigrator migrator() {
        RowMapper<Object[]> mapper = (rs, rowNum) -> {
            String name = rs.getString("name");
            if ("bad".equals(name)) {
                throw new SQLException("unsupported name");
            }
            return new Object[] {rs.getLong("id"), name};
        };
        return KeysetTableMigrator.builder()
            .table("listing")
            .source(jdbcTemplate)
            .sourceSelectSql("SELECT id, name, created_at FROM listing_src WHERE id > ? {since} ORDER BY id LIMIT ?")
            .sourceCountSql("SELECT count(*) FROM listing_src WHERE 1 = 1 {since}")
            .sinceColumn("created_at")
            .keyColumn("id")
            .rowMapper(mapper)
            .target(jdbcTemplate)
            .targetTransactions(transactions)
            .targetInsertSql("INSERT INTO listing (id, name) VALUES (?, ?) ON CONFLICT DO NOTHING")
            .build();
    }

    private void insertSource(long fromId, int count, Instant createdAt) {
        for (long id = fromId; id < fromId + count; id++) {
            jdbcTemplate.update("INSERT INTO listing_src (id, name, created_at) VALUES (?, ?, ?)",
                id, "name-" + id, Timestamp.from(createdAt));
        }
    }

    private int targetRowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM listing", Integer.class);
    }

    private static MigrationSettings settings(int batchSize, Long limit, Instant since, boolean dryRun, List<String> tables) {
        return new MigrationSettings(batchSize, limit, since, dryRun, tables);
    }
}
