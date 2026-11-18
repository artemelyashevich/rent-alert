package com.rentalert.ops_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.migration.MigrationReply;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.ops_ms.migration.MigrationSagaOrchestrator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MigrationReplyListenerTests {

    private final MigrationSagaOrchestrator orchestrator = mock(MigrationSagaOrchestrator.class);
    private final MigrationReplyListener listener = createListener();

    private MigrationReplyListener createListener() {
        MigrationReplyListener created = new MigrationReplyListener(orchestrator);
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
        listener.onReply(new MigrationReply(UUID.randomUUID(), 1, "listing-ms", true, List.of(), null));

        verify(orchestrator).onReply(any(), any());
    }

    @Test
    void duplicateReplyIsProcessedOnce() {
        MigrationReply reply = new MigrationReply(UUID.randomUUID(), 1, "listing-ms", true, List.of(), null);

        listener.onReply(reply);
        listener.onReply(reply);

        verify(orchestrator, times(1)).onReply(any(), any());
    }

    @Test
    void replyWithoutMigrationIdIsNotRetried() {
        assertThatThrownBy(() -> listener.onReply(new MigrationReply(null, 1, "listing-ms", true, List.of(), null)))
            .isInstanceOf(NonRetryableEventException.class);
        verify(orchestrator, never()).onReply(any(), any());
    }
}
