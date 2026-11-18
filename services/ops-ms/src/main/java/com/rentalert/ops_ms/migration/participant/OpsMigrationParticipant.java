package com.rentalert.ops_ms.migration.participant;

import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobScope;
import com.rentalert.dto.event.job.JobStatus;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.migration.AbstractMonolithMigrationParticipant;
import com.rentalert.kafka.migration.MigrationContext;
import com.rentalert.kafka.migration.MigrationTable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OpsMigrationParticipant extends AbstractMonolithMigrationParticipant {

    private static final String AUDIT_LOG = "audit_log";
    private static final String JOB_SETTING = "job_setting";
    private static final String USER_STATISTICS = "user_statistics";
    private static final String SINCE_PLACEHOLDER = "{since}";
    private static final String SINCE_COLUMN = "created_at";
    private static final String USER_STATISTICS_DUPLICATES_SQL =
        "SELECT count(*) FROM (SELECT user_profile_id, COALESCE(user_filter_id, 0), event_type, event_date"
            + " FROM public.user_statistics WHERE 1 = 1 {since} GROUP BY 1, 2, 3, 4 HAVING count(*) > 1) d";

    public OpsMigrationParticipant() {
        super("ops-ms");
    }

    @Override
    protected List<MigrationTable> tables() {
        return List.of(
            MigrationTable.builder()
                .name(AUDIT_LOG)
                .sourceTable("public.audit_log")
                .columns(List.of("id", "event_name", "event_description", "user_id", "event_initiator", "event_status",
                    "created_at", "updated_at"))
                .sinceColumn(SINCE_COLUMN)
                .build(),
            MigrationTable.builder()
                .name(JOB_SETTING)
                .sourceTable("public.job_setting")
                .columns(List.of("id", "job_name", "initiator", "description", "execution_scope", "affected_records",
                    "started_at", "finished_at", "execution_time_ms", "status", "error_message", "created_at", "updated_at"))
                .sinceColumn(SINCE_COLUMN)
                .build(),
            MigrationTable.builder()
                .name(USER_STATISTICS)
                .sourceTable("public.user_statistics")
                .columns(List.of("id", "user_id", "filter_id", "event_type", "event_date", "count", "created_at", "updated_at"))
                .selectExpressions(Map.of(
                    "user_id", "user_profile_id AS user_id",
                    "filter_id", "user_filter_id AS filter_id"))
                .sinceColumn(SINCE_COLUMN)
                .build());
    }

    @Override
    protected void preflight(MigrationContext context, JdbcTemplate source) {
        Instant since = context.settings().since();
        if (isSelected(context, AUDIT_LOG)) {
            assertAllowed("audit_log_event_initiator_precheck", AUDIT_LOG, "event_initiator", EventInitiatorType.values(), since);
            assertAllowed("audit_log_event_status_precheck", AUDIT_LOG, "event_status", EventStatus.values(), since);
        }
        if (isSelected(context, JOB_SETTING)) {
            assertAllowed("job_setting_job_name_precheck", JOB_SETTING, "job_name", JobName.values(), since);
            assertAllowed("job_setting_initiator_precheck", JOB_SETTING, "initiator", JobInitiator.values(), since);
            assertAllowed("job_setting_execution_scope_precheck", JOB_SETTING, "execution_scope", JobScope.values(), since);
            assertAllowed("job_setting_status_precheck", JOB_SETTING, "status", JobStatus.values(), since);
        }
        if (isSelected(context, USER_STATISTICS)) {
            assertAllowed("user_statistics_event_type_precheck", USER_STATISTICS, "event_type", UserEvent.values(), since);
            assertNoViolations("user_statistics_null_filter_duplicates_precheck", USER_STATISTICS_DUPLICATES_SQL, since, List.of());
        }
    }

    private void assertAllowed(String label, String table, String column, Enum<?>[] allowed, Instant since) {
        List<String> names = Arrays.stream(allowed).map(Enum::name).toList();
        String sql = "SELECT count(*) FROM public." + table + " WHERE 1 = 1 {since} AND " + column
            + " NOT IN (" + String.join(", ", Collections.nCopies(names.size(), "?")) + ")";
        assertNoViolations(label, sql, since, names);
    }

    private void assertNoViolations(String label, String sql, Instant since, List<String> values) {
        List<Object> args = new ArrayList<>();
        if (since != null) {
            args.add(Timestamp.from(since));
        }
        args.addAll(values);
        String resolved = sql.replace(SINCE_PLACEHOLDER, since == null ? "" : "AND " + SINCE_COLUMN + " >= ?");
        long violations = countSource(resolved, args.toArray());
        if (violations != 0) {
            throw new IllegalStateException("[" + label + "] " + violations
                + " monolith row(s) violate the ops-ms schema; fix the source before migrating");
        }
    }

    private static boolean isSelected(MigrationContext context, String table) {
        List<String> tables = context.settings().tables();
        return tables == null || tables.isEmpty() || tables.contains(table);
    }
}
