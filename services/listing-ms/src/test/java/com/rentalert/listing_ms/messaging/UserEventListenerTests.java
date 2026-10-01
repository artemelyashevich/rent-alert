package com.rentalert.listing_ms.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.listing_ms.service.UserDataService;
import org.junit.jupiter.api.Test;

class UserEventListenerTests {

    private final UserDataService userDataService = mock(UserDataService.class);
    private final UserEventListener listener = new UserEventListener(userDataService);

    @Test
    void deletedUserDataIsRemoved() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.USER_DELETED).build();
        event.setInitiatorId(5L);

        listener.onUserAction(event);

        verify(userDataService).deleteUserData(5L);
    }

    @Test
    void otherEventsAreIgnored() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.FAVORITE_ADDED).build();
        event.setInitiatorId(5L);

        listener.onUserAction(event);

        verify(userDataService, never()).deleteUserData(any());
    }
}
