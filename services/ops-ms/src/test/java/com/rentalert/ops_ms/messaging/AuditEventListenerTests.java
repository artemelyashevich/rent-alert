package com.rentalert.ops_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.ops_ms.service.AuditLogService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuditEventListenerTests {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AuditEventListener listener = createListener();

    private AuditEventListener createListener() {
        AuditEventListener created = new AuditEventListener(auditLogService);
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
        AdminActionEvent event = AdminActionEvent.builder().event(AdminEvent.JOB_CLEAN_UP).build();

        listener.onAdminAction(event);

        verify(auditLogService).record(event);
    }

    @Test
    void duplicateEventIsRecordedOnce() {
        AdminActionEvent event = AdminActionEvent.builder().event(AdminEvent.JOB_CLEAN_UP).build();

        listener.onAdminAction(event);
        listener.onAdminAction(event);

        verify(auditLogService, times(1)).record(event);
    }

    @Test
    void eventWithoutTypeIsNotRetried() {
        assertThatThrownBy(() -> listener.onAdminAction(new AdminActionEvent()))
            .isInstanceOf(NonRetryableEventException.class);
        verify(auditLogService, never()).record(any());
    }
}
