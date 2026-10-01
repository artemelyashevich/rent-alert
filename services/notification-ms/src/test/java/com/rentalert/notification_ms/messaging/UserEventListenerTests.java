package com.rentalert.notification_ms.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.notification_ms.service.NotificationSettingsService;
import com.rentalert.notification_ms.service.UserDataService;
import org.junit.jupiter.api.Test;

class UserEventListenerTests {

    private final UserDataService userDataService = mock(UserDataService.class);
    private final NotificationSettingsService notificationSettingsService = mock(NotificationSettingsService.class);
    private final UserEventListener listener = new UserEventListener(userDataService, notificationSettingsService);

    private static UserActionEvent event(UserEvent type) {
        UserActionEvent event = UserActionEvent.builder().event(type).build();
        event.setInitiatorId(5L);
        return event;
    }

    @Test
    void deletedUserDataIsRemoved() {
        listener.onUserAction(event(UserEvent.USER_DELETED));

        verify(userDataService).deleteUserData(5L);
        verifyNoInteractions(notificationSettingsService);
    }

    @Test
    void registeredUserGetsDefaultSettings() {
        listener.onUserAction(event(UserEvent.USER_REGISTERED));

        verify(notificationSettingsService).createDefault(5L);
        verifyNoInteractions(userDataService);
    }

    @Test
    void otherEventsAreIgnored() {
        listener.onUserAction(event(UserEvent.FAVORITE_ADDED));

        verify(userDataService, never()).deleteUserData(any());
        verify(notificationSettingsService, never()).createDefault(any());
    }
}
