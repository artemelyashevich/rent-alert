package com.rentalert.identity_ms.migration;

import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.migration.AbstractMonolithMigrationParticipant;
import com.rentalert.kafka.migration.MigrationContext;
import com.rentalert.kafka.migration.MigrationTable;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdentityMigrationParticipant extends AbstractMonolithMigrationParticipant {

    private static final String USER_PROFILES = "user_profiles";
    private static final String USER_SETTINGS = "user_settings";
    private static final String USER_FILTERS = "user_filters";
    private static final String FEEDBACKS = "feedbacks";
    private static final String SINCE_COLUMN = "updated_at";

    private static final String USER_FILTERS_INSERT = "INSERT INTO user_filters (id, user_profile_id, price_from, price_to, rooms_from, rooms_to, area_from, area_to,"
        + " is_active, created_at, updated_at, currency, city, owner_match_id, category)"
        + " SELECT s.id, s.user_profile_id, s.price_from, s.price_to, s.rooms_from, s.rooms_to, s.area_from, s.area_to, s.is_active,"
        + " s.created_at, s.updated_at, s.currency, s.city, s.owner_match_id, s.category"
        + " FROM (SELECT CAST(? AS BIGINT) AS id, CAST(? AS BIGINT) AS user_profile_id, CAST(? AS INTEGER) AS price_from,"
        + " CAST(? AS INTEGER) AS price_to, CAST(? AS INTEGER) AS rooms_from, CAST(? AS INTEGER) AS rooms_to,"
        + " CAST(? AS DOUBLE PRECISION) AS area_from, CAST(? AS DOUBLE PRECISION) AS area_to, CAST(? AS BOOLEAN) AS is_active,"
        + " CAST(? AS TIMESTAMP WITH TIME ZONE) AS created_at, CAST(? AS TIMESTAMP WITH TIME ZONE) AS updated_at,"
        + " CAST(? AS VARCHAR(5)) AS currency, CAST(? AS VARCHAR(255)) AS city, CAST(? AS BIGINT) AS owner_match_id,"
        + " CAST(? AS VARCHAR(10)) AS category) s"
        + " WHERE NOT EXISTS (SELECT 1 FROM user_filters t WHERE t.id = s.id) ON CONFLICT (id) DO NOTHING";

    @Autowired
    private JdbcTemplate targetTemplate;

    public IdentityMigrationParticipant() {
        super("identity-ms");
    }

    @Override
    protected List<MigrationTable> tables() {
        return List.of(
            MigrationTable.builder()
                .name(USER_PROFILES)
                .sourceTable("public.user_profiles")
                .columns(List.of("id", "created_at", "updated_at", "telegram_id", "telegram_id_hash", "username", "last_login",
                    "photo_url"))
                .sinceColumn(SINCE_COLUMN)
                .build(),
            MigrationTable.builder()
                .name(USER_SETTINGS)
                .sourceTable("public.user_settings")
                .columns(List.of("id", "user_profile_id", "email", "phone_number", "color_theme", "language", "refresh_token",
                    "password_hash", "role_name", "bot_status", "quiet_from", "quiet_to", "is_email_active", "created_at",
                    "updated_at"))
                .selectExpressions(Map.of("bot_status", "bot_status::text AS bot_status"))
                .insertPlaceholders(Map.of("bot_status", "CAST(? AS bot_status_enum)"))
                .sinceColumn(SINCE_COLUMN)
                .build(),
            MigrationTable.builder()
                .name(USER_FILTERS)
                .sourceTable("public.user_filters")
                .columns(List.of("id", "user_profile_id", "price_from", "price_to", "rooms_from", "rooms_to", "area_from",
                    "area_to", "is_active", "created_at", "updated_at", "currency", "city", "owner_match_id", "category"))
                .sinceColumn(SINCE_COLUMN)
                .customInsertSql(USER_FILTERS_INSERT)
                .build(),
            MigrationTable.builder()
                .name(FEEDBACKS)
                .sourceTable("public.feedbacks")
                .columns(List.of("id", "user_id", "message", "rating", "created_at", "updated_at", "subject_type", "subject_id",
                    "reason_code"))
                .sinceColumn(SINCE_COLUMN)
                .build());
    }

    @Override
    protected void preflight(MigrationContext context, JdbcTemplate source) {
        MigrationSettings settings = context.settings();
        if (settings.dryRun()) {
            return;
        }
        if (isSelected(settings, USER_PROFILES)) {
            guarded(USER_PROFILES, () -> checkUserProfiles(source));
        }
        if (isSelected(settings, USER_SETTINGS)) {
            guarded(USER_SETTINGS, () -> checkUserSettings(source));
        }
    }

    @Override
    protected List<TableResult> runTable(MigrationTable table, MigrationContext context) {
        try {
            List<TableResult> results = super.runTable(table, context);
            results.forEach(result -> verifyCount(table.name(), result, context.settings()));
            return results;
        } catch (DataAccessException e) {
            throw new IllegalStateException(table.name() + " migration failed: " + describe(e));
        }
    }

    private void checkUserProfiles(JdbcTemplate source) {
        warnIfAny(USER_PROFILES + ": telegram_id looks plaintext, not AES-GCM ciphertext; identity-ms cannot decrypt it",
            "SELECT count(*) FROM public.user_profiles WHERE telegram_id ~ '^[0-9]+$'");
        warnIfAny(USER_PROFILES + ": seeded admin profile keeps a non-encrypted telegram_id",
            "SELECT count(*) FROM public.user_profiles WHERE telegram_id LIKE 'admin-seed-%'");
        warnIfAny(USER_PROFILES + ": telegram_id_hash is NULL; identity-ms entity maps it as NOT NULL and looks profiles up by it",
            "SELECT count(*) FROM public.user_profiles WHERE telegram_id_hash IS NULL");
    }

    private void checkUserSettings(JdbcTemplate source) {
        assertBotStatusValues(source);
        warnIfAny(USER_SETTINGS + ": refresh_token looks like a plaintext JWT, not AES-GCM ciphertext; identity-ms decrypt will fail",
            "SELECT count(*) FROM public.user_settings WHERE refresh_token LIKE 'eyJ%'");
    }

    private void assertBotStatusValues(JdbcTemplate source) {
        Set<String> targetLabels = new HashSet<>(targetTemplate.queryForList(
            "SELECT enumlabel FROM pg_enum WHERE enumtypid = 'bot_status_enum'::regtype", String.class));
        List<String> missing = source.queryForList(
                "SELECT DISTINCT bot_status::text FROM public.user_settings WHERE bot_status IS NOT NULL", String.class)
            .stream()
            .filter(value -> !targetLabels.contains(value))
            .sorted()
            .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("user_settings.bot_status values " + missing
                + " are missing in target bot_status_enum; add them with an identity-ms schema migration"
                + " (ALTER TYPE bot_status_enum ADD VALUE ...) and re-run");
        }
    }

    private void verifyCount(String table, TableResult result, MigrationSettings settings) {
        if (settings.dryRun() || settings.since() != null || settings.limitPerTable() != null) {
            return;
        }
        Long target = targetTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
        long targetCount = target == null ? 0 : target;
        if (targetCount != result.total()) {
            throw new IllegalStateException(table + ": source=" + result.total() + " target=" + targetCount + " MISMATCH");
        }
    }

    private static void guarded(String table, Runnable check) {
        try {
            check.run();
        } catch (DataAccessException e) {
            throw new IllegalStateException(table + " migration failed: " + describe(e));
        }
    }

    private static boolean isSelected(MigrationSettings settings, String table) {
        List<String> tables = settings.tables();
        return tables == null || tables.isEmpty() || tables.contains(table);
    }

    private static String describe(DataAccessException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return e.getClass().getSimpleName() + " SQLState=" + sqlException.getSQLState();
            }
        }
        return e.getClass().getSimpleName();
    }
}
