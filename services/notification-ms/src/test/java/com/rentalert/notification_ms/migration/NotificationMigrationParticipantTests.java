package com.rentalert.notification_ms.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.migration.MigrationContext;
import com.rentalert.kafka.migration.MonolithSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

class NotificationMigrationParticipantTests {

    private static final Pattern FROM_TABLE = Pattern.compile("FROM public\\.(\\w+)");
    private static final List<String> ALL_TABLES = List.of("notification_broadcasts", "notification_broadcast_recipients",
        "user_notification_settings", "telegram_messages", "notifications", "reminders");
    private static final Instant SINCE = Instant.parse("2026-03-01T00:00:00Z");

    private JdbcTemplate source;
    private JdbcTemplate target;
    private TransactionTemplate transactions;
    private ObjectProvider<MonolithSource> provider;
    private MigrationContext context;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        source = mock(JdbcTemplate.class);
        target = mock(JdbcTemplate.class);
        transactions = mock(TransactionTemplate.class);
        provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(new MonolithSource(source));
        context = mock(MigrationContext.class);
        when(context.settings()).thenReturn(settings(null, List.of()));
    }

    @Test
    void failsWhenMonolithSourceIsNotConfigured() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> participant().run(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("migration is disabled: set app.migration.enabled=true");
    }

    @Test
    void migratesTablesInScriptOrder() {
        List<TableResult> results = participant().run(context);

        assertThat(countedTables()).containsExactlyElementsOf(ALL_TABLES);
        assertThat(results).hasSize(ALL_TABLES.size());
    }

    @Test
    void tableFilterSkipsOtherTablesAndTheirPrechecks() {
        when(context.settings()).thenReturn(settings(null, List.of("reminders")));

        participant().run(context);

        assertThat(countedTables()).containsExactly("reminders");
        assertThat(allSourceSql()).noneMatch(sql -> sql.contains("notification_broadcast") || sql.contains("telegram_messages")
            || sql.contains("public.notifications "));
    }

    @Test
    void sinceIsAcceptedByTablesWithoutSinceColumn() {
        when(context.settings()).thenReturn(settings(SINCE, List.of()));

        List<TableResult> results = participant().run(context);

        assertThat(results).hasSize(ALL_TABLES.size());
    }

    @Test
    void recipientStatusViolationStopsBeforeTableIsWritten() {
        when(context.settings()).thenReturn(settings(null, List.of("notification_broadcast_recipients")));
        when(source.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenAnswer(invocation -> invocation.<String>getArgument(0).contains("status NOT IN") ? 1L : 0L);

        assertThatThrownBy(() -> participant().run(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("[notification_broadcast_recipients_status_precheck] 1 monolith row(s) violate the notification-ms schema; "
                + "fix the source before migrating");
        assertThat(countedTables()).isEmpty();
    }

    @Test
    void duplicateSuccessDeduplicationKeysStopNotificationsMigration() {
        when(context.settings()).thenReturn(settings(null, List.of("notifications")));
        when(source.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
            .thenAnswer(invocation -> invocation.<String>getArgument(0).contains("HAVING") ? 2L : 0L);

        assertThatThrownBy(() -> participant().run(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("[notifications_success_dedup_precheck] 2 monolith row(s)");
        assertThat(countedTables()).isEmpty();
    }

    @Test
    void unmappedReminderTemplatesAreCheckedInPreflightBeforeReminderRowsAreCopied() {
        when(context.settings()).thenReturn(settings(null, List.of("reminders")));
        when(source.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of(5L));

        participant().run(context);

        assertThat(allSourceSql()).anyMatch(sql -> sql.contains("notification_template_id IN (?)"));
        assertThat(countedTables()).containsExactly("reminders");
    }

    private Participant participant() {
        Participant participant = new Participant();
        ReflectionTestUtils.setField(participant, "target", target);
        ReflectionTestUtils.setField(participant, "jdbcTemplate", target);
        ReflectionTestUtils.setField(participant, "monolithSource", provider);
        ReflectionTestUtils.setField(participant, "targetTransactions", transactions);
        return participant;
    }

    private static final class Participant extends NotificationMigrationParticipant {

        List<TableResult> run(MigrationContext context) {
            return migrate(context);
        }
    }

    private List<String> countedTables() {
        List<String> tables = new ArrayList<>();
        for (Invocation invocation : mockingDetails(source).getInvocations()) {
            if (!invocation.getMethod().getName().equals("queryForObject")) {
                continue;
            }
            String sql = invocation.getArgument(0);
            if (!sql.contains("WHERE 1 = 1")) {
                continue;
            }
            Matcher matcher = FROM_TABLE.matcher(sql);
            if (matcher.find()) {
                tables.add(matcher.group(1));
            }
        }
        return tables;
    }

    private List<String> allSourceSql() {
        List<String> sql = new ArrayList<>();
        for (Invocation invocation : mockingDetails(source).getInvocations()) {
            Object first = invocation.getArguments().length > 0 ? invocation.getArguments()[0] : null;
            if (first instanceof String text) {
                sql.add(text);
            }
        }
        return sql;
    }

    private static MigrationSettings settings(Instant since, List<String> tables) {
        return new MigrationSettings(1000, null, since, false, tables);
    }
}
