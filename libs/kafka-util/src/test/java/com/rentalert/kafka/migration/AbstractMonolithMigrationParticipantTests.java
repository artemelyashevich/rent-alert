package com.rentalert.kafka.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

@SuppressWarnings("unchecked")
class AbstractMonolithMigrationParticipantTests {

    private static final Instant OLD = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NEW = Instant.parse("2026-06-01T00:00:00Z");
    private static final MigrationSettings ALL = new MigrationSettings(2, null, null, false, List.of());

    private final MigrationControl control = mock(MigrationControl.class);
    private final ObjectProvider<MonolithSource> monolithSource = mock(ObjectProvider.class);
    private final UUID migrationId = UUID.randomUUID();
    private DataSource sourceDataSource;
    private DataSource targetDataSource;
    private JdbcTemplate source;
    private JdbcTemplate target;

    @BeforeEach
    void setUp() {
        sourceDataSource = dataSource("src");
        targetDataSource = dataSource("dst");
        source = new JdbcTemplate(sourceDataSource);
        target = new JdbcTemplate(targetDataSource);
        source.execute("CREATE TABLE user_profiles (user_id BIGINT PRIMARY KEY, username VARCHAR(100), updated_at TIMESTAMP)");
        source.execute("CREATE TABLE user_filters (user_id BIGINT, category VARCHAR(20), PRIMARY KEY (user_id, category))");
        target.execute("CREATE TABLE user_profiles (user_id BIGINT PRIMARY KEY, username VARCHAR(100), updated_at TIMESTAMP)");
        target.execute("CREATE TABLE user_filters (user_id BIGINT, category VARCHAR(20), PRIMARY KEY (user_id, category))");
        when(monolithSource.getIfAvailable()).thenReturn(new MonolithSource(source));
    }

    @AfterEach
    void tearDown() {
        source.execute("SHUTDOWN");
        target.execute("SHUTDOWN");
    }

    @Test
    void migratesEveryTableInOrderThroughTheBase() {
        insertProfile(1, OLD);
        insertProfile(2, NEW);
        insertProfile(3, NEW);
        source.update("INSERT INTO user_filters (user_id, category) VALUES (1, 'a'), (1, 'b'), (2, 'a')");

        List<TableResult> results = participant(profiles(), filters()).migrate(context(ALL));

        assertThat(results).containsExactly(
            new TableResult("user_profiles", 3, 3, 0, 0),
            new TableResult("user_filters", 3, 3, 0, 0));
        assertThat(target.queryForList("SELECT user_id FROM user_profiles ORDER BY user_id", Long.class))
            .containsExactly(1L, 2L, 3L);
        assertThat(target.queryForObject("SELECT count(*) FROM user_filters", Long.class)).isEqualTo(3L);
    }

    @Test
    void sinceFilterIsAppliedToTheTable() {
        insertProfile(1, OLD);
        insertProfile(2, NEW);
        MigrationSettings since = new MigrationSettings(10, null, Instant.parse("2026-03-01T00:00:00Z"), false, List.of());

        List<TableResult> results = participant(profiles()).migrate(context(since));

        assertThat(results).containsExactly(new TableResult("user_profiles", 1, 1, 0, 0));
        assertThat(target.queryForList("SELECT user_id FROM user_profiles", Long.class)).containsExactly(2L);
    }

    @Test
    void missingMonolithSourceFailsWithDisabledMessage() {
        when(monolithSource.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> participant(profiles()).migrate(context(ALL)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("migration is disabled: set app.migration.enabled=true");
    }

    @Test
    void cancellationStopsTheTable() {
        insertProfile(1, OLD);
        insertProfile(2, OLD);
        insertProfile(3, OLD);
        when(control.isCancelled(migrationId)).thenReturn(true);

        assertThatThrownBy(() -> participant(profiles()).migrate(context(ALL)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cancelled");
        assertThat(target.queryForObject("SELECT count(*) FROM user_profiles", Long.class)).isZero();
    }

    @Test
    void requireNoneReportsCountAndIdsWithoutFieldValues() {
        insertProfile(7, OLD);
        insertProfile(9, OLD);
        AbstractMonolithMigrationParticipant participant = participant(profiles());

        assertThatThrownBy(() -> participant.requireNone("usernames", "SELECT user_id FROM user_profiles ORDER BY user_id"))
            .isInstanceOf(MigrationPreflightException.class)
            .hasMessage("usernames: 2 row(s) violate the precondition; ids [7, 9]");
    }

    @Test
    void requireNonePassesWhenNoRowsViolate() {
        insertProfile(1, OLD);
        AbstractMonolithMigrationParticipant participant = participant(profiles());

        participant.requireNone("usernames", "SELECT user_id FROM user_profiles WHERE user_id > 5");
    }

    @Test
    void requireNoneLimitsIdsPreview() {
        for (long id = 1; id <= 25; id++) {
            insertProfile(id, OLD);
        }
        AbstractMonolithMigrationParticipant participant = participant(profiles());

        assertThatThrownBy(() -> participant.requireNone("all", "SELECT user_id FROM user_profiles"))
            .isInstanceOf(MigrationPreflightException.class)
            .hasMessageStartingWith("all: 25 row(s) violate the precondition; ids [1, 2, 3")
            .satisfies(e -> assertThat(e.getMessage().split(",")).hasSize(20));
    }

    @Test
    void warnIfAnyDoesNotFailAndCountReturnsSourceCount() {
        insertProfile(1, OLD);
        insertProfile(2, OLD);
        AbstractMonolithMigrationParticipant participant = participant(profiles());

        assertThat(participant.countSource("SELECT count(*) FROM user_profiles")).isEqualTo(2L);
        participant.warnIfAny("nulls", "SELECT count(*) FROM user_profiles WHERE username IS NULL");
    }

    private void insertProfile(long id, Instant updatedAt) {
        source.update("INSERT INTO user_profiles (user_id, username, updated_at) VALUES (?, ?, ?)", id, "u" + id,
            Timestamp.from(updatedAt));
    }

    private MigrationTable profiles() {
        return MigrationTable.builder()
            .name("user_profiles")
            .columns(List.of("user_id", "username", "updated_at"))
            .keyColumns(List.of("user_id"))
            .conflictTarget(List.of())
            .sinceColumn("updated_at")
            .build();
    }

    private MigrationTable filters() {
        return MigrationTable.builder()
            .name("user_filters")
            .columns(List.of("user_id", "category"))
            .keyColumns(List.of("user_id", "category"))
            .conflictTarget(List.of())
            .build();
    }

    private AbstractMonolithMigrationParticipant participant(MigrationTable... tables) {
        List<MigrationTable> list = List.of(tables);
        AbstractMonolithMigrationParticipant participant = new AbstractMonolithMigrationParticipant("listing-ms") {
            @Override
            protected List<MigrationTable> tables() {
                return list;
            }
        };
        ReflectionTestUtils.setField(participant, "target", target);
        ReflectionTestUtils.setField(participant, "monolithSource", monolithSource);
        ReflectionTestUtils.setField(participant, "targetTransactions",
            new TransactionTemplate(new DataSourceTransactionManager(targetDataSource)));
        return participant;
    }

    private MigrationContext context(MigrationSettings settings) {
        return new MigrationContext(migrationId, 1, "listing-ms", settings, control, progress -> { });
    }

    private static DataSource dataSource(String name) {
        return new DriverManagerDataSource("jdbc:h2:mem:" + name + "-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    }
}
