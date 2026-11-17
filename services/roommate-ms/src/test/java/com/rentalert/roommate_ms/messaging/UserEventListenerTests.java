package com.rentalert.roommate_ms.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.roommate_ms.service.RoommateUserDataService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserEventListenerTests {

    private final RoommateUserDataService userDataService = mock(RoommateUserDataService.class);
    private final UserEventListener listener = createListener();

    private UserEventListener createListener() {
        UserEventListener created = new UserEventListener(userDataService);
        ReflectionTestUtils.setField(created, "inboxGuard", new InMemoryInboxGuard());
        return created;
    }

    @Test
    void deletedUserDataIsRemoved() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.USER_DELETED).build();
        event.setInitiatorId(5L);

        listener.onUserAction(event);

        verify(userDataService).deleteUserData(5L);
    }

    @Test
    void otherEventsAreIgnored() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.USER_REGISTERED).build();
        event.setInitiatorId(5L);

        listener.onUserAction(event);

        verifyNoInteractions(userDataService);
    }

    @Test
    void deletionWithoutInitiatorIsIgnored() {
        listener.onUserAction(UserActionEvent.builder().event(UserEvent.USER_DELETED).build());

        verifyNoInteractions(userDataService);
    }

    @Test
    void redeliveredDeletionIsProcessedOnce() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.USER_DELETED).build();
        event.setInitiatorId(5L);

        listener.onUserAction(event);
        listener.onUserAction(event);

        verify(userDataService, times(1)).deleteUserData(5L);
    }
}
