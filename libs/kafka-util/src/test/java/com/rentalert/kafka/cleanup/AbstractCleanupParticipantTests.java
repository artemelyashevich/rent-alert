package com.rentalert.kafka.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.cleanup.CleanupCommand;
import com.rentalert.dto.event.cleanup.CleanupReply;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class AbstractCleanupParticipantTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final Instant reference = Instant.parse("2026-09-30T21:00:00Z");
    private Instant cleanedWith;
    private final AbstractCleanupParticipant participant = new AbstractCleanupParticipant() {
        @Override
        protected CleanupResult cleanUp(Instant referenceTime) {
            cleanedWith = referenceTime;
            return new CleanupResult(5, 1, Duration.ofMillis(40), List.of());
        }
    };

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ReflectionTestUtils.setField(participant, "outboxPublisher", outboxPublisher);
        ReflectionTestUtils.setField(participant, "transactionTemplate", transactionTemplate);
        ReflectionTestUtils.setField(participant, "serviceName", "listing-ms");
        doAnswer(invocation -> {
            invocation.<Consumer<TransactionStatus>>getArgument(0).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void runsOwnStepAndReplies() {
        UUID sagaId = UUID.randomUUID();

        participant.onCommand(new CleanupCommand(sagaId, 1, "listing-ms", reference));

        assertThat(cleanedWith).isEqualTo(reference);
        ArgumentCaptor<Object> reply = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.CLEANUP_REPLIES), eq(sagaId.toString()), reply.capture());
        assertThat(reply.getValue()).isEqualTo(new CleanupReply(sagaId, 1, "listing-ms", 5, 1, 40, List.of()));
    }

    @Test
    void ignoresStepOfAnotherService() {
        participant.onCommand(new CleanupCommand(UUID.randomUUID(), 2, "processing-ms", reference));

        assertThat(cleanedWith).isNull();
        verifyNoInteractions(outboxPublisher);
    }
}
