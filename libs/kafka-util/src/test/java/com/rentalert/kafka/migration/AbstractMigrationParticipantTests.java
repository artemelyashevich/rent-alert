package com.rentalert.kafka.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.migration.MigrationCommand;
import com.rentalert.dto.event.migration.MigrationReply;
import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class AbstractMigrationParticipantTests {

    private final MigrationControl migrationControl = mock(MigrationControl.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final MigrationSettings settings = new MigrationSettings(100, null, null, false, List.of());
    private final UUID migrationId = UUID.randomUUID();

    private List<TableResult> tables = List.of(new TableResult("listing", 5, 5, 0, 0));
    private RuntimeException failure;
    private int migrations;

    private final AbstractMigrationParticipant participant = new AbstractMigrationParticipant("listing-ms") {
        @Override
        protected List<TableResult> migrate(MigrationContext context) {
            migrations++;
            if (failure != null) {
                throw failure;
            }
            return tables;
        }
    };

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(participant, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(participant, "migrationControl", migrationControl);
        ReflectionTestUtils.setField(participant, "outboxPublisher", outboxPublisher);
        ReflectionTestUtils.setField(participant, "transactionTemplate", transactionTemplate);
        doAnswer(invocation -> {
            invocation.<Consumer<TransactionStatus>>getArgument(0).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(migrationControl.tryWithLock(any(), any(), any())).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(2).run();
            return true;
        });
    }

    @Test
    void commandForAnotherServiceIsIgnored() {
        participant.onCommand(new MigrationCommand(migrationId, 1, "identity-ms", settings));

        assertThat(migrations).isZero();
        verifyNoInteractions(migrationControl, outboxPublisher);
    }

    @Test
    void successIsRepliedThroughOutboxWithTableResults() {
        participant.onCommand(new MigrationCommand(migrationId, 3, "listing-ms", settings));

        ArgumentCaptor<Object> reply = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.MIGRATION_REPLIES), eq(migrationId.toString()), reply.capture());
        assertThat(reply.getValue()).isEqualTo(new MigrationReply(migrationId, 3, "listing-ms", true, tables, null));
    }

    @Test
    void failureIsRepliedWithErrorMessageOnly() {
        failure = new IllegalStateException("source is unavailable");

        participant.onCommand(new MigrationCommand(migrationId, 3, "listing-ms", settings));

        ArgumentCaptor<Object> reply = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.MIGRATION_REPLIES), eq(migrationId.toString()), reply.capture());
        assertThat(reply.getValue()).isEqualTo(new MigrationReply(migrationId, 3, "listing-ms", false, List.of(),
            "IllegalStateException: source is unavailable"));
    }

    @Test
    void commandIsSkippedWhenSameMigrationIsAlreadyRunning() {
        doReturn(false).when(migrationControl).tryWithLock(any(), any(), any());

        participant.onCommand(new MigrationCommand(migrationId, 3, "listing-ms", settings));

        assertThat(migrations).isZero();
        verifyNoInteractions(outboxPublisher);
    }

    @Test
    void progressIsSentWithMigrationKey() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        AbstractMigrationParticipant reporting = new AbstractMigrationParticipant("listing-ms") {
            @Override
            protected List<TableResult> migrate(MigrationContext context) {
                context.progress("listing", 5, 5);
                return List.of();
            }
        };
        ReflectionTestUtils.setField(reporting, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(reporting, "migrationControl", migrationControl);
        ReflectionTestUtils.setField(reporting, "outboxPublisher", outboxPublisher);
        ReflectionTestUtils.setField(reporting, "transactionTemplate", transactionTemplate);

        reporting.onCommand(new MigrationCommand(migrationId, 3, "listing-ms", settings));

        verify(kafkaTemplate).send(eq(KafkaTopics.MIGRATION_PROGRESS), eq(migrationId.toString()), any());
    }
}
