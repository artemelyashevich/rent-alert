package com.rentalert.notification_ms.migration;

import com.rentalert.kafka.migration.AbstractMonolithMigrationParticipant;
import com.rentalert.kafka.migration.MigrationContext;
import com.rentalert.kafka.migration.MigrationPreflightException;
import com.rentalert.kafka.migration.MigrationTable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationMigrationParticipant extends AbstractMonolithMigrationParticipant {

    private static final String BROADCASTS_TABLE = "notification_broadcasts";
    private static final String RECIPIENTS_TABLE = "notification_broadcast_recipients";
    private static final String SETTINGS_TABLE = "user_notification_settings";
    private static final String TELEGRAM_MESSAGES_TABLE = "telegram_messages";
    private static final String NOTIFICATIONS_TABLE = "notifications";
    private static final String REMINDERS_TABLE = "reminders";

    private static final List<String> BROADCAST_COLUMNS = List.of("id", "created_by", "message", "total_recipients",
        "sent_count", "failed_count", "skipped_count", "created_at", "updated_at", "pinned");
    private static final List<String> RECIPIENT_COLUMNS = List.of("id", "broadcast_id", "user_id", "status",
        "error_message", "created_at", "updated_at");
    private static final List<String> SETTING_COLUMNS = List.of("id", "user_id", "engine", "is_enabled", "is_default",
        "created_at", "updated_at");
    private static final List<String> TELEGRAM_MESSAGE_COLUMNS = List.of("id", "user_id", "text", "command",
        "created_at", "updated_at");
    private static final List<String> NOTIFICATION_COLUMNS = List.of("id", "engine", "receiver", "user_id", "content",
        "status", "created_at", "updated_at", "attempt", "error_message", "next_retry_at", "deduplication_key",
        "retry_type", "template_params_json", "listing_id");
    private static final List<String> REMINDER_COLUMNS = List.of("id", "user_id", "listing_id", "roommate_viewing_id",
        "remind_at", "status", "notification_template_id", "interval_type", "sent_at", "attempts", "error", "created_at",
        "updated_at");

    private static final List<String> ENGINES = List.of("TELEGRAM_ADMIN", "EMAIL", "PUSH", "TELEGRAM_BOT", "WEBSOCKET", "SMS");
    private static final List<String> NOTIFICATION_STATUSES = List.of("PENDING", "SUCCESS", "FAILURE", "EXHAUSTED", "QUEUED");
    private static final List<String> NOTIFICATION_TYPES = List.of("APP_STARTED", "APP_STOPPED", "NEW_LISTING_ADMIN_ALERT",
        "USER_CREATED", "NEW_LISTING_MATCHED", "FAVORITE_REMINDER", "EMAIL_ACTIVATION", "EMAIL_ACTIVATION_SUCCESS",
        "NEW_FEEDBACK", "BOT_BLOCKED", "BOT_UNBLOCKED", "ADMIN_BROADCAST", "ROOMMATE_NEW_INTEREST",
        "ROOMMATE_NEW_CANDIDATES", "ROOMMATE_MATCHED", "ROOMMATE_CHAT_STARTED", "ROOMMATE_VIEWING_SCHEDULED",
        "ROOMMATE_VIEWING_REMINDER", "ROOMMATE_MATCH_ENDED", "ROOMMATE_INTEREST_EXPIRED", "ROOMMATE_PROFILE_APPROVED",
        "ROOMMATE_PROFILE_REJECTED");
    private static final List<String> REMINDER_STATUSES = List.of("NEW", "PROCESSING", "SENT", "FAILED", "CANCELLED");
    private static final List<String> REMINDER_INTERVALS = List.of("BEFORE_30_MIN", "BEFORE_1_HOUR", "BEFORE_1_DAY");
    private static final List<String> BROADCAST_RECIPIENT_STATUSES = List.of("SENT", "FAILED", "SKIPPED");
    private static final List<String> TELEGRAM_COMMANDS = List.of("START", "APP", "HELP");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TemplateDictionary templates;

    public NotificationMigrationParticipant() {
        super("notification-ms");
    }

    @Override
    protected void preflight(MigrationContext context, JdbcTemplate source) {
        Instant since = context.settings().since();
        if (isSelected(context, RECIPIENTS_TABLE)) {
            requireAllowed("notification_broadcast_recipients_status_precheck", RECIPIENTS_TABLE, "status",
                BROADCAST_RECIPIENT_STATUSES, null, since);
        }
        if (isSelected(context, SETTINGS_TABLE)) {
            requireAllowed("user_notification_settings_engine_precheck", SETTINGS_TABLE, "engine", ENGINES, null, since);
        }
        if (isSelected(context, TELEGRAM_MESSAGES_TABLE)) {
            requireAllowed("telegram_messages_command_precheck", TELEGRAM_MESSAGES_TABLE, "command", TELEGRAM_COMMANDS,
                "created_at", since);
        }
        if (isSelected(context, NOTIFICATIONS_TABLE)) {
            requireAllowed("notifications_engine_precheck", NOTIFICATIONS_TABLE, "engine", ENGINES, "created_at", since);
            requireAllowed("notifications_status_precheck", NOTIFICATIONS_TABLE, "status", NOTIFICATION_STATUSES,
                "created_at", since);
            requireAllowed("notifications_retry_type_precheck", NOTIFICATIONS_TABLE, "retry_type", NOTIFICATION_TYPES,
                "created_at", since);
            requireNoSuccessDuplicates(since);
        }
        if (isSelected(context, REMINDERS_TABLE)) {
            requireAllowed("reminders_status_precheck", REMINDERS_TABLE, "status", REMINDER_STATUSES, null, since);
            requireAllowed("reminders_interval_precheck", REMINDERS_TABLE, "interval_type", REMINDER_INTERVALS, null, since);
            templates = TemplateDictionary.load(source, jdbcTemplate);
            warnUnmappedTemplates();
        }
    }

    @Override
    protected List<MigrationTable> tables() {
        List<MigrationTable> tables = new ArrayList<>();
        tables.add(table(BROADCASTS_TABLE, BROADCAST_COLUMNS, "created_at"));
        tables.add(MigrationTable.builder()
            .name(RECIPIENTS_TABLE)
            .columns(RECIPIENT_COLUMNS)
            .sinceColumn("b.created_at")
            .customSelectSql("SELECT r.id, r.broadcast_id, r.user_id, r.status, r.error_message, r.created_at, r.updated_at "
                + "FROM public.notification_broadcast_recipients r JOIN public.notification_broadcasts b ON b.id = r.broadcast_id "
                + "WHERE r.id > ? {since} ORDER BY r.id LIMIT ?")
            .customCountSql("SELECT count(*) FROM public.notification_broadcast_recipients r JOIN public.notification_broadcasts b "
                + "ON b.id = r.broadcast_id WHERE 1 = 1 {since}")
            .build());
        tables.add(table(SETTINGS_TABLE, SETTING_COLUMNS, null));
        tables.add(table(TELEGRAM_MESSAGES_TABLE, TELEGRAM_MESSAGE_COLUMNS, "created_at"));
        tables.add(table(NOTIFICATIONS_TABLE, NOTIFICATION_COLUMNS, "created_at"));
        tables.add(MigrationTable.builder()
            .name(REMINDERS_TABLE)
            .sourceTable("public." + REMINDERS_TABLE)
            .columns(REMINDER_COLUMNS)
            .customRowMapper(NotificationRowMappers.reminders(REMINDER_COLUMNS, templates))
            .build());
        return tables;
    }

    private void warnUnmappedTemplates() {
        List<Long> unmapped = idsSource("SELECT DISTINCT notification_template_id FROM public.reminders "
            + "WHERE notification_template_id IS NOT NULL").stream()
            .filter(id -> templates.targetId(id) == null)
            .toList();
        if (unmapped.isEmpty()) {
            return;
        }
        long rows = countSource("SELECT count(*) FROM public.reminders WHERE notification_template_id IN ("
            + placeholders(unmapped.size()) + ")", unmapped.toArray());
        log.warn("[MIGRATION] {} reminder(s) reference a monolith template with no (notification_type, engine) match "
            + "in notification-ms; notification_template_id is left NULL for them", rows);
    }

    private void requireAllowed(String label, String table, String column, List<String> allowed, String sinceColumn,
        Instant since) {
        boolean windowed = sinceColumn != null && since != null;
        String sql = "SELECT count(*) FROM public." + table + " WHERE TRUE"
            + (windowed ? " AND " + sinceColumn + " >= ?" : "")
            + " AND " + column + " NOT IN (" + placeholders(allowed.size()) + ")";
        List<Object> args = new ArrayList<>();
        if (windowed) {
            args.add(Timestamp.from(since));
        }
        args.addAll(allowed);
        requireNoViolations(label, countSource(sql, args.toArray()));
    }

    private void requireNoSuccessDuplicates(Instant since) {
        String window = since == null ? "" : " AND created_at >= ?";
        String sql = "SELECT count(*) FROM (SELECT deduplication_key, engine FROM public.notifications "
            + "WHERE status = 'SUCCESS' AND deduplication_key IS NOT NULL" + window
            + " GROUP BY deduplication_key, engine HAVING count(*) > 1) d";
        requireNoViolations("notifications_success_dedup_precheck",
            countSource(sql, since == null ? new Object[0] : new Object[] {Timestamp.from(since)}));
    }

    private static void requireNoViolations(String label, long violations) {
        if (violations > 0) {
            throw new MigrationPreflightException("[" + label + "] " + violations
                + " monolith row(s) violate the notification-ms schema; fix the source before migrating");
        }
        log.info("[MIGRATION] [{}] source precheck OK", label);
    }

    private static MigrationTable table(String name, List<String> columns, String sinceColumn) {
        return MigrationTable.builder()
            .name(name)
            .sourceTable("public." + name)
            .columns(columns)
            .sinceColumn(sinceColumn)
            .build();
    }

    private static boolean isSelected(MigrationContext context, String table) {
        List<String> tables = context.settings().tables();
        return tables == null || tables.isEmpty() || tables.contains(table);
    }

    private static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }
}
