package com.rentalert.identity_ms.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.migration.MigrationContext;
import com.rentalert.kafka.migration.MonolithSource;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class IdentityMigrationParticipantTests {

    private static final List<String> ALL_TABLES = List.of("user_profiles", "user_settings", "user_filters", "feedbacks");
    private static final String USER_SETTINGS_SELECT = "SELECT id, user_profile_id, email, phone_number, color_theme, language,"
        + " refresh_token, password_hash, role_name, bot_status::text AS bot_status, quiet_from, quiet_to, is_email_active,"
        + " created_at, updated_at FROM public.user_settings WHERE id > ?  ORDER BY id LIMIT ?";
    private static final String USER_SETTINGS_INSERT = "INSERT INTO user_settings (id, user_profile_id, email, phone_number,"
        + " color_theme, language, refresh_token, password_hash, role_name, bot_status, quiet_from, quiet_to, is_email_active,"
        + " created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS bot_status_enum), ?, ?, ?, ?, ?)"
        + " ON CONFLICT (id) DO NOTHING";

    private final JdbcTemplate target = mock(JdbcTemplate.class);
    private final JdbcTemplate monolith = mock(JdbcTemplate.class);
    private final MigrationContext context = mock(MigrationContext.class);
    private final TransactionTemplate transactions = new TransactionTemplate(mock(PlatformTransactionManager.class));

    @Test
    void failsFastWhenMonolithSourceIsAbsent() {
        Probe participant = participant(withoutMonolith());
        when(context.settings()).thenReturn(settings(false, null));

        assertThatThrownBy(() -> participant.run(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("migration is disabled: set app.migration.enabled=true");
        verify(target, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    void migratesTablesInScriptOrder() {
        Probe participant = participant(withMonolith());
        when(context.settings()).thenReturn(settings(false, null));

        List<TableResult> results = participant.run(context);

        assertThat(results).extracting(TableResult::table).containsExactlyElementsOf(ALL_TABLES);
    }

    @Test
    void settingsTablesFilterSkipsUnselectedTables() {
        Probe participant = participant(withMonolith());
        when(context.settings()).thenReturn(settings(false, List.of("feedbacks")));

        List<TableResult> results = participant.run(context);

        assertThat(results).extracting(TableResult::table).containsExactly("feedbacks");
    }

    @Test
    void userSettingsKeepBotStatusCastInSelectAndInsert() {
        ResultSet row = mock(ResultSet.class);
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(2);
            handler.processRow(row);
            return null;
        }).when(monolith).query(anyString(), any(Object[].class), any(RowCallbackHandler.class));
        when(target.batchUpdate(anyString(), anyList())).thenReturn(new int[] {1});
        Probe participant = participant(withMonolith());
        when(context.settings()).thenReturn(settings(false, List.of("user_settings")));

        participant.run(context);

        verify(monolith).query(eq(USER_SETTINGS_SELECT), any(Object[].class), any(RowCallbackHandler.class));
        verify(target).batchUpdate(eq(USER_SETTINGS_INSERT), anyList());
    }

    @Test
    void unknownBotStatusFailsBeforeUserSettingsAreWritten() {
        stubBotStatuses(List.of("ACTIVE", "RETIRED"));
        Probe participant = participant(withMonolith());
        when(context.settings()).thenReturn(settings(false, List.of("user_settings")));

        assertThatThrownBy(() -> participant.run(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("[RETIRED]")
            .hasMessageContaining("bot_status_enum");
        verify(target, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    void dryRunSkipsChecksAndWrites() {
        stubBotStatuses(List.of("ACTIVE", "RETIRED"));
        Probe participant = participant(withMonolith());
        when(context.settings()).thenReturn(settings(true, List.of("user_settings")));

        List<TableResult> results = participant.run(context);

        assertThat(results).extracting(TableResult::table).containsExactly("user_settings");
        verify(target, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    void plaintextWarningsDoNotBlockMigration() {
        when(monolith.queryForObject(contains("telegram_id ~"), eq(Long.class), any(Object[].class))).thenReturn(3L);
        Probe participant = participant(withMonolith());
        when(context.settings()).thenReturn(settings(false, List.of("user_profiles")));

        List<TableResult> results = participant.run(context);

        assertThat(results).extracting(TableResult::table).containsExactly("user_profiles");
    }

    @Test
    void rowCountMismatchFailsTable() {
        when(target.queryForObject(eq("SELECT count(*) FROM feedbacks"), eq(Long.class))).thenReturn(5L);
        Probe participant = participant(withMonolith());
        when(context.settings()).thenReturn(settings(false, List.of("feedbacks")));

        assertThatThrownBy(() -> participant.run(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("feedbacks: source=0 target=5 MISMATCH");
    }

    @Test
    void databaseErrorMessageDoesNotExposeRowValues() {
        ResultSet row = mock(ResultSet.class);
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(2);
            handler.processRow(row);
            return null;
        }).when(monolith).query(anyString(), any(Object[].class), any(RowCallbackHandler.class));
        when(target.batchUpdate(anyString(), anyList()))
            .thenThrow(new DuplicateKeyException("Key (user_id)=(secret-telegram-42) already exists"));
        Probe participant = participant(withMonolith());
        when(context.settings()).thenReturn(settings(false, List.of("feedbacks")));

        assertThatThrownBy(() -> participant.run(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("feedbacks migration failed: DuplicateKeyException");
    }

    private void stubBotStatuses(List<String> monolithStatuses) {
        when(target.queryForList(contains("pg_enum"), eq(String.class))).thenReturn(List.of("ACTIVE", "PAUSED", "STOPPED"));
        when(monolith.queryForList(contains("DISTINCT bot_status"), eq(String.class))).thenReturn(monolithStatuses);
    }

    private Probe participant(ObjectProvider<MonolithSource> source) {
        Probe participant = new Probe();
        ReflectionTestUtils.setField(participant, "target", target);
        ReflectionTestUtils.setField(participant, "targetTemplate", target);
        ReflectionTestUtils.setField(participant, "monolithSource", source);
        ReflectionTestUtils.setField(participant, "targetTransactions", transactions);
        return participant;
    }

    private ObjectProvider<MonolithSource> withMonolith() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("monolithSource", new MonolithSource(monolith));
        return factory.getBeanProvider(MonolithSource.class);
    }

    private static ObjectProvider<MonolithSource> withoutMonolith() {
        return new DefaultListableBeanFactory().getBeanProvider(MonolithSource.class);
    }

    private static MigrationSettings settings(boolean dryRun, List<String> tables) {
        return new MigrationSettings(0, null, null, dryRun, tables);
    }

    private static final class Probe extends IdentityMigrationParticipant {

        List<TableResult> run(MigrationContext context) {
            return migrate(context);
        }
    }
}
