package com.rentalert.processing_ms.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.migration.MigrationCommand;
import com.rentalert.dto.event.migration.MigrationReply;
import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.migration.MigrationControl;
import com.rentalert.kafka.migration.MonolithSource;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ProcessingMigrationParticipantTests {

    private static final List<String> SCRIPT_ORDER = List.of(
        "cities", "scheduler_settings", "schedule_tasks", "scheduled_task_records", "currency_rates", "listings");
    private static final int INSERT_STATEMENTS = 8;
    private static final int PHOTOS_INDEX = 19;

    private final UUID migrationId = UUID.randomUUID();
    private final JdbcTemplate targetJdbc = mock(JdbcTemplate.class);
    private final JdbcTemplate sourceJdbc = mock(JdbcTemplate.class);
    private final ResultSet resultSet = mock(ResultSet.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final MigrationControl control = mock(MigrationControl.class);
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final TransactionTemplate transactions = new TransactionTemplate(mock(PlatformTransactionManager.class));

    @Test
    void disabledMigrationRepliesWithFailure() {
        ProcessingMigrationParticipant participant = participant(absentMonolith());

        participant.onCommand(command(List.of()));

        MigrationReply reply = reply();
        assertThat(reply.success()).isFalse();
        assertThat(reply.error()).contains("migration is disabled: set app.migration.enabled=true");
        assertThat(reply.tables()).isEmpty();
    }

    @Test
    void migratesTablesInScriptOrder() {
        ProcessingMigrationParticipant participant = participant(presentMonolith());

        participant.onCommand(command(List.of()));

        MigrationReply reply = reply();
        assertThat(reply.success()).isTrue();
        assertThat(reply.tables()).extracting(TableResult::table).containsExactlyElementsOf(SCRIPT_ORDER);
    }

    @Test
    void tablesFilterSelectsOnlyRequestedTables() {
        ProcessingMigrationParticipant participant = participant(presentMonolith());

        participant.onCommand(command(List.of("currency_rates")));

        MigrationReply reply = reply();
        assertThat(reply.success()).isTrue();
        assertThat(reply.tables()).extracting(TableResult::table).containsExactly("currency_rates");
    }

    @Test
    void mergesTwoPassesOfNaturalKeyTable() {
        TableResult withId = new TableResult("cities", 5, 3, 2, 1);
        TableResult withoutId = new TableResult("cities", 5, 1, 0, 0);

        assertThat(ProcessingMigrationParticipant.merge(withId, withoutId))
            .isEqualTo(new TableResult("cities", 5, 4, 1, 1));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void everyInsertHasOnePlaceholderPerRowColumn() {
        ProcessingMigrationParticipant participant = participant(presentMonolith());

        participant.onCommand(command(List.of()));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> rows = ArgumentCaptor.forClass(List.class);
        verify(targetJdbc, times(INSERT_STATEMENTS)).batchUpdate(sql.capture(), rows.capture());
        for (int i = 0; i < INSERT_STATEMENTS; i++) {
            assertThat(placeholders(sql.getAllValues().get(i)))
                .isEqualTo(((Object[]) rows.getAllValues().get(i).get(0)).length);
        }
    }

    @Test
    void listingPhotosArrayIsPassedAsTextAndCastToVarcharArray() throws SQLException {
        when(resultSet.getString("photos")).thenReturn("{a.jpg,b.jpg}");

        ListingInsert insert = listingInsert();

        assertThat(insert.sql()).contains("CAST(? AS VARCHAR[])");
        assertThat(insert.row()[PHOTOS_INDEX]).isEqualTo("{a.jpg,b.jpg}");
    }

    @Test
    void nullListingPhotosStayNull() {
        ListingInsert insert = listingInsert();

        assertThat(insert.row()[PHOTOS_INDEX]).isNull();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ListingInsert listingInsert() {
        ProcessingMigrationParticipant participant = participant(presentMonolith());

        participant.onCommand(command(List.of("listings")));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> rows = ArgumentCaptor.forClass(List.class);
        verify(targetJdbc).batchUpdate(sql.capture(), rows.capture());
        return new ListingInsert(sql.getValue(), (Object[]) rows.getValue().get(0));
    }

    private record ListingInsert(String sql, Object[] row) {
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<MonolithSource> absentMonolith() {
        ObjectProvider<MonolithSource> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<MonolithSource> presentMonolith() {
        doAnswer(invocation -> {
            ((RowCallbackHandler) invocation.getArgument(2)).processRow(resultSet);
            return null;
        }).when(sourceJdbc).query(anyString(), any(Object[].class), any(RowCallbackHandler.class));
        when(targetJdbc.batchUpdate(anyString(), anyList()))
            .thenAnswer(invocation -> new int[((List<?>) invocation.getArgument(1)).size()]);
        ObjectProvider<MonolithSource> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(new MonolithSource(sourceJdbc));
        return provider;
    }

    private ProcessingMigrationParticipant participant(ObjectProvider<MonolithSource> monolith) {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return true;
        }).when(control).tryWithLock(anyString(), any(), any());
        ProcessingMigrationParticipant participant = new ProcessingMigrationParticipant();
        ReflectionTestUtils.setField(participant, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(participant, "migrationControl", control);
        ReflectionTestUtils.setField(participant, "monolithSource", monolith);
        ReflectionTestUtils.setField(participant, "outboxPublisher", outboxPublisher);
        ReflectionTestUtils.setField(participant, "target", targetJdbc);
        ReflectionTestUtils.setField(participant, "targetTransactions", transactions);
        ReflectionTestUtils.setField(participant, "transactionTemplate", transactions);
        return participant;
    }

    private MigrationCommand command(List<String> tables) {
        return new MigrationCommand(migrationId, 1, "processing-ms",
            new MigrationSettings(MigrationSettings.DEFAULT_BATCH_SIZE, null, null, false, tables));
    }

    private MigrationReply reply() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.MIGRATION_REPLIES), eq(migrationId.toString()), captor.capture());
        return (MigrationReply) captor.getValue();
    }

    private static int placeholders(String sql) {
        return (int) sql.chars().filter(c -> c == '?').count();
    }
}
