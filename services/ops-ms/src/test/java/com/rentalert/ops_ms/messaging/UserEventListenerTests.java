package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.ops_ms.service.AuditLogService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserEventListenerTests {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final UserEventListener listener = new UserEventListener(auditLogService);

    @Test
    void recordsValidEvent() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.FAVORITE_ADDED).build();

        listener.onUserAction(event);

        verify(auditLogService).recordUserAction(event);
    }

    @Test
    void skipsListingReceived() {
        listener.onUserAction(UserActionEvent.builder().event(UserEvent.LISTING_RECEIVED).build());

        verify(auditLogService, never()).recordUserAction(any());
    }

    @Test
    void eventWithoutTypeIsNotRetried() {
        assertThatThrownBy(() -> listener.onUserAction(new UserActionEvent()))
            .isInstanceOf(NonRetryableEventException.class);
        verify(auditLogService, never()).recordUserAction(any());
    }
}
