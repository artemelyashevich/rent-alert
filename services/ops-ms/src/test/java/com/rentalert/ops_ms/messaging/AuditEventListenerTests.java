package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.ops_ms.service.AuditLogService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuditEventListenerTests {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AuditEventListener listener = new AuditEventListener(auditLogService);

    @Test
    void recordsValidEvent() {
        AdminActionEvent event = AdminActionEvent.builder().event(AdminEvent.JOB_CLEAN_UP).build();

        listener.onAdminAction(event);

        verify(auditLogService).record(event);
    }

    @Test
    void eventWithoutTypeIsNotRetried() {
        assertThatThrownBy(() -> listener.onAdminAction(new AdminActionEvent()))
            .isInstanceOf(NonRetryableEventException.class);
        verify(auditLogService, never()).record(any());
    }
}
