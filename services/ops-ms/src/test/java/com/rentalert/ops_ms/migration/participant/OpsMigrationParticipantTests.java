package com.rentalert.ops_ms.migration.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.migration.MigrationContext;
import com.rentalert.kafka.migration.MigrationTable;
import com.rentalert.kafka.migration.MonolithSource;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class OpsMigrationParticipantTests {

    private static final Instant SINCE = Instant.parse("2026-03-01T00:00:00Z");
    private static final String DISABLED_MESSAGE = "migration is disabled: set app.migration.enabled=true";

    private final JdbcTemplate target = mock(JdbcTemplate.class);
    private final JdbcTemplate monolith = mock(JdbcTemplate.class);
    private final MigrationContext context = mock(MigrationContext.class);
    private final ObjectProvider<MonolithSource> monolithSource = mock(ObjectProvider.class);
    private final TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));

    private OpsMigrationParticipant participant;

    @BeforeEach
    void setUp() {
        when(monolithSource.getIfAvailable()).thenReturn(new MonolithSource(monolith));
        when(context.settings()).thenReturn(settings(null, List.of()));
        participant = new OpsMigrationParticipant();
        ReflectionTestUtils.setField(participant, "target", target);
        ReflectionTestUtils.setField(participant, "monolithSource", monolithSource);
        ReflectionTestUtils.setField(participant, "targetTransactions", transactionTemplate);
    }

    @Test
    void failsWhenMonolithSourceIsAbsent() {
        when(monolithSource.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(this::migrate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(DISABLED_MESSAGE);
        verify(target, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    void migratesAuditLogJobSettingThenUserStatistics() {
        List<TableResult> results = migrate();

        assertThat(results).extracting(TableResult::table).containsExactly("audit_log", "job_setting", "user_statistics");
    }

    @Test
    void resetsSequenceOfEachMigratedTable() {
        migrate();

        verify(target).execute(contains("'audit_log', 'id'"));
        verify(target).execute(contains("'job_setting', 'id'"));
        verify(target).execute(contains("'user_statistics', 'id'"));
    }

    @Test
    void respectsTableFilterFromSettings() {
        when(context.settings()).thenReturn(settings(null, List.of("job_setting")));

        List<TableResult> results = migrate();

        assertThat(results).extracting(TableResult::table).containsExactly("job_setting");
    }

    @Test
    void appliesSinceFilterByCreatedAtToEveryQuery() {
        when(context.settings()).thenReturn(settings(SINCE, List.of()));

        migrate();

        verify(monolith, atLeastOnce())
            .queryForObject(contains("created_at >= ?"), eq(Long.class), any(Object[].class));
        verify(monolith, times(3))
            .query(contains("created_at >= ?"), any(Object[].class), any(RowCallbackHandler.class));
    }

    @Test
    void auditLogEventInitiatorViolationStopsBeforeWrite() {
        when(monolith.queryForObject(contains("event_initiator NOT IN"), eq(Long.class), any(Object[].class))).thenReturn(1L);

        assertThatThrownBy(this::migrate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("[audit_log_event_initiator_precheck] 1 monolith row(s) violate the ops-ms schema; fix the source before migrating");
        verify(target, never()).batchUpdate(anyString(), anyList());
        verify(target, never()).execute(anyString());
    }

    @Test
    void jobSettingStatusViolationStopsBeforeWrite() {
        when(monolith.queryForObject(argThat(sql -> sql.contains("public.job_setting") && sql.contains("status NOT IN")),
            eq(Long.class), any(Object[].class))).thenReturn(2L);

        assertThatThrownBy(this::migrate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("[job_setting_status_precheck] 2 monolith row(s) violate the ops-ms schema; fix the source before migrating");
        verify(target, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    void userEventViolationStopsBeforeWrite() {
        when(monolith.queryForObject(contains("event_type NOT IN"), eq(Long.class), any(Object[].class))).thenReturn(3L);

        assertThatThrownBy(this::migrate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("[user_statistics_event_type_precheck] 3 monolith row(s) violate the ops-ms schema; fix the source before migrating");
        verify(target, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    void duplicateStatisticsWithNullFilterStopsBeforeWrite() {
        when(monolith.queryForObject(contains("HAVING count(*) > 1"), eq(Long.class), any(Object[].class))).thenReturn(4L);

        assertThatThrownBy(this::migrate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("[user_statistics_null_filter_duplicates_precheck] 4 monolith row(s) violate the ops-ms schema; fix the source before migrating");
        verify(target, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    void skippedTableRunsNoPrecheck() {
        when(monolith.queryForObject(contains("public.audit_log"), eq(Long.class), any(Object[].class))).thenReturn(9L);
        when(context.settings()).thenReturn(settings(null, List.of("job_setting", "user_statistics")));

        List<TableResult> results = migrate();

        assertThat(results).extracting(TableResult::table).containsExactly("job_setting", "user_statistics");
    }

    @Test
    void auditLogSqlMatchesTargetSchema() {
        assertThat(sql("audit_log", "insertSql")).isEqualTo("INSERT INTO audit_log (id, event_name, event_description, user_id,"
            + " event_initiator, event_status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING");
        assertThat(sql("audit_log", "selectSql")).isEqualTo("SELECT id, event_name, event_description, user_id, event_initiator,"
            + " event_status, created_at, updated_at FROM public.audit_log WHERE id > ? {since} ORDER BY id LIMIT ?");
        assertThat(sql("audit_log", "countSql")).isEqualTo("SELECT count(*) FROM public.audit_log WHERE 1 = 1 {since}");
    }

    @Test
    void jobSettingSqlMatchesTargetSchema() {
        assertThat(sql("job_setting", "insertSql")).isEqualTo("INSERT INTO job_setting (id, job_name, initiator, description,"
            + " execution_scope, affected_records, started_at, finished_at, execution_time_ms, status, error_message, created_at,"
            + " updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING");
        assertThat(sql("job_setting", "selectSql")).isEqualTo("SELECT id, job_name, initiator, description, execution_scope,"
            + " affected_records, started_at, finished_at, execution_time_ms, status, error_message, created_at, updated_at"
            + " FROM public.job_setting WHERE id > ? {since} ORDER BY id LIMIT ?");
    }

    @Test
    void userStatisticsRenamesProfileAndFilterColumnsInSelect() {
        assertThat(sql("user_statistics", "insertSql")).isEqualTo("INSERT INTO user_statistics (id, user_id, filter_id,"
            + " event_type, event_date, count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING");
        assertThat(sql("user_statistics", "selectSql")).isEqualTo("SELECT id, user_profile_id AS user_id, user_filter_id AS filter_id,"
            + " event_type, event_date, count, created_at, updated_at FROM public.user_statistics WHERE id > ? {since} ORDER BY id LIMIT ?");
    }

    private String sql(String tableName, String method) {
        for (MigrationTable table : participant.tables()) {
            if (table.name().equals(tableName)) {
                return ReflectionTestUtils.invokeMethod(table, method);
            }
        }
        throw new IllegalArgumentException("no table " + tableName);
    }

    private List<TableResult> migrate() {
        return ReflectionTestUtils.invokeMethod(participant, "migrate", context);
    }

    private static MigrationSettings settings(Instant since, List<String> tables) {
        return new MigrationSettings(MigrationSettings.DEFAULT_BATCH_SIZE, null, since, false, tables);
    }
}
