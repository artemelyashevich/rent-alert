package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.identity_ms.service.BotStatusService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserEventListenerTests {

    private final BotStatusService botStatusService = mock(BotStatusService.class);
    private final UserEventListener listener = new UserEventListener(botStatusService);

    @Test
    void marksBotBlocked() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.BOT_BLOCKED).build();
        event.setInitiatorId(10L);

        listener.onUserAction(event);

        verify(botStatusService).markBotBlocked(10L);
    }

    @Test
    void ignoresOtherEvents() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.LISTING_RECEIVED).build();
        event.setInitiatorId(10L);

        listener.onUserAction(event);

        verifyNoInteractions(botStatusService);
    }
}
