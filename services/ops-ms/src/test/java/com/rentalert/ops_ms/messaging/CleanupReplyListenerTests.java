package com.rentalert.ops_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.cleanup.CleanupReply;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.ops_ms.cleanup.CleanupSagaOrchestrator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CleanupReplyListenerTests {

    private final CleanupSagaOrchestrator orchestrator = mock(CleanupSagaOrchestrator.class);
    private final CleanupReplyListener listener = createListener();

    private CleanupReplyListener createListener() {
        CleanupReplyListener created = new CleanupReplyListener(orchestrator);
        InboxGuard inboxGuard = mock(InboxGuard.class);
        Set<UUID> processed = new HashSet<>();
        when(inboxGuard.processOnce(any(), any(), any())).thenAnswer(invocation -> {
            if (!processed.add(invocation.<UUID>getArgument(1))) {
                return false;
            }
            invocation.<Runnable>getArgument(2).run();
            return true;
        });
        ReflectionTestUtils.setField(created, "inboxGuard", inboxGuard);
        return created;
    }

    @Test
    void replyIsPassedToOrchestrator() {
        CleanupReply reply = new CleanupReply(UUID.randomUUID(), 1, "listing-ms", 3, 0, 10, List.of());

        listener.onReply(reply);

        verify(orchestrator).onReply(any(), any());
    }

    @Test
    void duplicateReplyIsProcessedOnce() {
        CleanupReply reply = new CleanupReply(UUID.randomUUID(), 1, "listing-ms", 3, 0, 10, List.of());

        listener.onReply(reply);
        listener.onReply(reply);

        verify(orchestrator, times(1)).onReply(any(), any());
    }

    @Test
    void replyWithoutSagaIdIsNotRetried() {
        assertThatThrownBy(() -> listener.onReply(new CleanupReply(null, 1, "listing-ms", 0, 0, 0, List.of())))
            .isInstanceOf(NonRetryableEventException.class);
        verify(orchestrator, never()).onReply(any(), any());
    }
}
