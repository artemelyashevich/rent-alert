package com.rentalert.ops_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.ops_ms.service.AuditLogService;
import com.rentalert.ops_ms.service.StatisticsService;
import org.junit.jupiter.api.Test;

class UserEventListenerTests {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final StatisticsService statisticsService = mock(StatisticsService.class);
    private final UserEventListener listener = new UserEventListener(auditLogService, statisticsService);

    @Test
    void recordsValidEvent() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.FAVORITE_ADDED).build();

        listener.onUserAction(event);

        verify(auditLogService).recordUserAction(event);
        verify(statisticsService).record(event);
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
}
