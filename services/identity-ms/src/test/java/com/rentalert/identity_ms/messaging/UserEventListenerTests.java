package com.rentalert.identity_ms.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.identity_ms.service.BotStatusService;
import com.rentalert.kafka.inbox.InboxGuard;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserEventListenerTests {

    private final BotStatusService botStatusService = mock(BotStatusService.class);
    private final InboxGuard inboxGuard = mock(InboxGuard.class);
    private final UserEventListener listener = createListener();

    private UserEventListener createListener() {
        UserEventListener created = new UserEventListener(botStatusService);
        ReflectionTestUtils.setField(created, "inboxGuard", inboxGuard);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(2).run();
            return true;
        }).when(inboxGuard).processOnce(any(), any(), any());
        return created;
    }

    private static UserActionEvent event(UserEvent type) {
        UserActionEvent event = UserActionEvent.builder().event(type).build();
        event.setInitiatorId(10L);
        return event;
    }

    @Test
    void marksBotBlocked() {
        listener.onUserAction(event(UserEvent.BOT_BLOCKED));

        verify(botStatusService).markBotBlocked(10L);
    }

    @Test
    void marksBotUnblocked() {
        listener.onUserAction(event(UserEvent.BOT_UNBLOCKED));

        verify(botStatusService).markBotUnblocked(10L);
    }

    @Test
    void ignoresOtherEvents() {
        listener.onUserAction(event(UserEvent.LISTING_RECEIVED));

        verifyNoInteractions(botStatusService);
    }

    @Test
    void eventIsProcessedThroughInboxWithEventKey() {
        UserActionEvent event = event(UserEvent.BOT_BLOCKED);

        listener.onUserAction(event);

        verify(inboxGuard).processOnce(eq("identity.user-events"), eq(event.getIdempotencyKey()), any(Runnable.class));
    }

    @Test
    void duplicateEventDoesNotRunHandlerAgain() {
        doReturn(false).when(inboxGuard).processOnce(any(), any(), any());

        listener.onUserAction(event(UserEvent.BOT_BLOCKED));

        verifyNoInteractions(botStatusService);
    }

    @Test
    void eventWithoutInitiatorIsSkippedBeforeInbox() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.BOT_BLOCKED).build();

        listener.onUserAction(event);

        verifyNoInteractions(inboxGuard, botStatusService);
    }
}
