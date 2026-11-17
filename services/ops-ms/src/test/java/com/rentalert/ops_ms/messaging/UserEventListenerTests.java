package com.rentalert.ops_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.ops_ms.service.AuditLogService;
import com.rentalert.ops_ms.service.StatisticsService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserEventListenerTests {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final StatisticsService statisticsService = mock(StatisticsService.class);
    private final UserEventListener listener = createListener();

    private UserEventListener createListener() {
        UserEventListener created = new UserEventListener(auditLogService, statisticsService);
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
    void recordsValidEvent() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.FAVORITE_ADDED).build();

        listener.onUserAction(event);

        verify(auditLogService).recordUserAction(event);
        verify(statisticsService).record(event);
    }

    @Test
    void duplicateEventIsRecordedOnce() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.FAVORITE_ADDED).build();

        listener.onUserAction(event);
        listener.onUserAction(event);

        verify(auditLogService, times(1)).recordUserAction(event);
        verify(statisticsService, times(1)).record(event);
    }

    @Test
    void listingReceivedGoesToStatisticsOnly() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.LISTING_RECEIVED).build();

        listener.onUserAction(event);

        verify(auditLogService, never()).recordUserAction(any());
        verify(statisticsService).record(event);
    }

    @Test
    void eventWithoutTypeIsNotRetried() {
        assertThatThrownBy(() -> listener.onUserAction(new UserActionEvent()))
            .isInstanceOf(NonRetryableEventException.class);
        verify(auditLogService, never()).recordUserAction(any());
        verify(statisticsService, never()).record(any());
    }

    @Test
    void deletedUserLosesStatisticsButKeepsAuditRecord() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.USER_DELETED).build();
        event.setInitiatorId(5L);

        listener.onUserAction(event);

        verify(auditLogService).recordUserAction(event);
        verify(statisticsService).deleteUser(5L);
        verify(statisticsService, never()).record(any());
    }
}
