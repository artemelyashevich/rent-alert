package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.ops_ms.service.AuditLogService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "ops.audit";

    private final AuditLogService auditLogService;

    public AuditEventListener(AuditLogService auditLogService) {
        super(CONSUMER);
        this.auditLogService = auditLogService;
    }

    @KafkaListener(topics = KafkaTopics.AUDIT_EVENTS, groupId = CONSUMER)
    public void onAdminAction(AdminActionEvent event) {
        if (event.getEvent() == null || event.getIdempotencyKey() == null) {
            throw new NonRetryableEventException("Admin action event without event or idempotency key");
        }
        processOnce(event.getIdempotencyKey(), () -> auditLogService.record(event));
    }
}
