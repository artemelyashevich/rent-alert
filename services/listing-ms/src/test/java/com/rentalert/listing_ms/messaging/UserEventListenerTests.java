package com.rentalert.listing_ms.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.listing_ms.service.UserDataService;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserEventListenerTests {

    private final UserDataService userDataService = mock(UserDataService.class);
    private final InboxGuard inboxGuard = mock(InboxGuard.class);
    private final Set<String> processed = new HashSet<>();
    private final UserEventListener listener = createListener();

    private UserEventListener createListener() {
        UserEventListener created = new UserEventListener(userDataService);
        ReflectionTestUtils.setField(created, "inboxGuard", inboxGuard);
        return created;
    }

    @BeforeEach
    void runHandlerOncePerKey() {
        doAnswer(invocation -> {
            if (!processed.add(invocation.getArgument(0) + ":" + invocation.getArgument(1))) {
                return false;
            }
            invocation.<Runnable>getArgument(2).run();
            return true;
        }).when(inboxGuard).processOnce(any(), any(), any(Runnable.class));
    }

    @Test
    void deletedUserDataIsRemoved() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.USER_DELETED).build();
        event.setInitiatorId(5L);

        listener.onUserAction(event);

        verify(userDataService).deleteUserData(5L);
    }

    @Test
    void redeliveredDeleteEventRemovesUserDataOnce() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.USER_DELETED).build();
        event.setInitiatorId(5L);

        listener.onUserAction(event);
        listener.onUserAction(event);

        verify(userDataService, times(1)).deleteUserData(5L);
    }

    @Test
    void otherEventsAreIgnored() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.FAVORITE_ADDED).build();
        event.setInitiatorId(5L);

        listener.onUserAction(event);

        verify(userDataService, never()).deleteUserData(any());
    }
}
