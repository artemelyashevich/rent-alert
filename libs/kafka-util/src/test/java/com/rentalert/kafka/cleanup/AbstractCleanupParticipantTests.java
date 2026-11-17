package com.rentalert.kafka.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.cleanup.CleanupCommand;
import com.rentalert.dto.event.cleanup.CleanupReply;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AbstractCleanupParticipantTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final InboxGuard inboxGuard = mock(InboxGuard.class);
    private final Instant reference = Instant.parse("2026-09-30T21:00:00Z");
    private final AtomicInteger cleanUps = new AtomicInteger();
    private Instant cleanedWith;
    private final AbstractCleanupParticipant participant = new AbstractCleanupParticipant("listing-ms") {
        @Override
        protected CleanupResult cleanUp(Instant referenceTime) {
            cleanedWith = referenceTime;
            cleanUps.incrementAndGet();
            return new CleanupResult(5, 1, Duration.ofMillis(40), List.of());
        }
    };

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(participant, "outboxPublisher", outboxPublisher);
        ReflectionTestUtils.setField(participant, "inboxGuard", inboxGuard);
        Set<UUID> processed = new HashSet<>();
        when(inboxGuard.processOnce(any(), any(), any())).thenAnswer(invocation -> {
            if (!processed.add(invocation.<UUID>getArgument(1))) {
                return false;
            }
            invocation.<Runnable>getArgument(2).run();
            return true;
        });
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
    void commandIsProcessedThroughInboxWithStepKey() {
        UUID sagaId = UUID.randomUUID();
        UUID expectedKey = UUID.nameUUIDFromBytes(("cleanup:" + sagaId + ":1:listing-ms").getBytes(StandardCharsets.UTF_8));

        participant.onCommand(new CleanupCommand(sagaId, 1, "listing-ms", reference));

        verify(inboxGuard).processOnce(eq("listing-ms"), eq(expectedKey), any(Runnable.class));
    }

    @Test
    void duplicateCommandIsNotCleanedOrRepliedTwice() {
        CleanupCommand command = new CleanupCommand(UUID.randomUUID(), 1, "listing-ms", reference);

        participant.onCommand(command);
        participant.onCommand(command);

        assertThat(cleanUps).hasValue(1);
        verify(outboxPublisher, times(1)).publish(eq(KafkaTopics.CLEANUP_REPLIES), any(), any());
    }

    @Test
    void commandsOfDifferentStepsAreBothProcessed() {
        UUID sagaId = UUID.randomUUID();

        participant.onCommand(new CleanupCommand(sagaId, 1, "listing-ms", reference));
        participant.onCommand(new CleanupCommand(sagaId, 2, "listing-ms", reference));

        assertThat(cleanUps).hasValue(2);
    }

    @Test
    void ignoresStepOfAnotherService() {
        participant.onCommand(new CleanupCommand(UUID.randomUUID(), 2, "processing-ms", reference));

        assertThat(cleanedWith).isNull();
        verifyNoInteractions(outboxPublisher, inboxGuard);
    }
}
