package com.rentalert.kafka.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MigrationTableTests {

    @Test
    void singleKeyGeneratesKeysetSelectCountAndInsert() {
        MigrationTable table = MigrationTable.builder()
            .name("user_profiles")
            .sourceTable("public.user_profiles")
            .columns(List.of("user_id", "username", "updated_at"))
            .keyColumns(List.of("user_id"))
            .sinceColumn("updated_at")
            .build();

        assertThat(table.selectSql()).isEqualTo(
            "SELECT user_id, username, updated_at FROM public.user_profiles WHERE user_id > ? {since} ORDER BY user_id LIMIT ?");
        assertThat(table.countSql()).isEqualTo("SELECT count(*) FROM public.user_profiles WHERE 1 = 1 {since}");
        assertThat(table.insertSql()).isEqualTo(
            "INSERT INTO user_profiles (user_id, username, updated_at) VALUES (?, ?, ?) ON CONFLICT (user_id) DO NOTHING");
    }

    @Test
    void compositeKeyGeneratesAfterPlaceholderAndConflictTarget() {
        MigrationTable table = MigrationTable.builder()
            .name("roommate_hidden")
            .sourceTable("public.roommate_hidden")
            .columns(List.of("user_id", "subject_type", "subject_id", "created_at"))
            .keyColumns(List.of("user_id", "subject_type", "subject_id"))
            .sinceColumn("created_at")
            .build();

        assertThat(table.selectSql()).isEqualTo("SELECT user_id, subject_type, subject_id, created_at FROM public.roommate_hidden"
            + " WHERE 1 = 1 {after} {since} ORDER BY user_id, subject_type, subject_id LIMIT ?");
        assertThat(table.insertSql()).isEqualTo("INSERT INTO roommate_hidden (user_id, subject_type, subject_id, created_at)"
            + " VALUES (?, ?, ?, ?) ON CONFLICT (user_id, subject_type, subject_id) DO NOTHING");
    }

    @Test
    void selectExpressionsInsertPlaceholdersArrayColumnsAndWhereAreApplied() {
        MigrationTable table = MigrationTable.builder()
            .name("user_settings")
            .sourceTable("public.user_settings")
            .columns(List.of("id", "bot_status", "lifestyle_tags"))
            .selectExpressions(Map.of("bot_status", "bot_status::text AS bot_status"))
            .insertPlaceholders(Map.of("bot_status", "CAST(? AS bot_status_enum)"))
            .arrayColumns(Set.of("lifestyle_tags"))
            .whereSource("is_active")
            .build();

        assertThat(table.selectSql()).isEqualTo("SELECT id, bot_status::text AS bot_status, lifestyle_tags FROM public.user_settings"
            + " WHERE id > ? AND (is_active) {since} ORDER BY id LIMIT ?");
        assertThat(table.countSql()).isEqualTo("SELECT count(*) FROM public.user_settings WHERE 1 = 1 AND (is_active) {since}");
        assertThat(table.insertSql()).isEqualTo("INSERT INTO user_settings (id, bot_status, lifestyle_tags)"
            + " VALUES (?, CAST(? AS bot_status_enum), CAST(? AS VARCHAR[])) ON CONFLICT (id) DO NOTHING");
    }

    @Test
    void fromSourceReplacesSourceTableInFromClause() {
        MigrationTable table = MigrationTable.builder()
            .name("roommate_viewings")
            .sourceTable("public.roommate_viewings")
            .columns(List.of("id", "listing_title"))
            .selectExpressions(Map.of("listing_title", "l.title AS listing_title"))
            .fromSource("public.roommate_viewings v LEFT JOIN public.listings l ON l.id = v.listing_id")
            .build();

        assertThat(table.selectSql()).isEqualTo("SELECT id, l.title AS listing_title FROM public.roommate_viewings v"
            + " LEFT JOIN public.listings l ON l.id = v.listing_id WHERE id > ? {since} ORDER BY id LIMIT ?");
    }

    @Test
    void emptyConflictTargetGeneratesPlainDoNothing() {
        MigrationTable table = MigrationTable.builder()
            .name("feedbacks")
            .columns(List.of("id", "message"))
            .conflictTarget(List.of())
            .build();

        assertThat(table.insertSql()).isEqualTo("INSERT INTO feedbacks (id, message) VALUES (?, ?) ON CONFLICT DO NOTHING");
    }

    @Test
    void customSqlReplacesGeneration() {
        MigrationTable table = MigrationTable.builder()
            .name("job_setting")
            .columns(List.of("id"))
            .customSelectSql("SELECT id FROM legacy WHERE id > ? {since} ORDER BY id LIMIT ?")
            .customInsertSql("INSERT INTO job_setting (id) VALUES (?)")
            .build();

        assertThat(table.selectSql()).isEqualTo("SELECT id FROM legacy WHERE id > ? {since} ORDER BY id LIMIT ?");
        assertThat(table.insertSql()).isEqualTo("INSERT INTO job_setting (id) VALUES (?)");
    }

    @Test
    void invalidIdentifierIsRejected() {
        assertThatThrownBy(() -> MigrationTable.builder()
            .name("users; DROP TABLE x")
            .columns(List.of("id"))
            .build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MigrationTable.builder()
            .name("users")
            .columns(List.of("id", "name) --"))
            .build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keyColumnOutsideColumnsIsRejected() {
        assertThatThrownBy(() -> MigrationTable.builder()
            .name("users")
            .columns(List.of("name"))
            .keyColumns(List.of("id"))
            .build()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("keyColumns");
    }
}
