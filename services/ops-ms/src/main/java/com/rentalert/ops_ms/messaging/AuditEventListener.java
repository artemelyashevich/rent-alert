package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.ops_ms.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogService auditLogService;

    @KafkaListener(topics = KafkaTopics.AUDIT_EVENTS, groupId = "ops.audit")
    public void onAdminAction(AdminActionEvent event) {
        if (event.getEvent() == null || event.getIdempotencyKey() == null) {
            throw new NonRetryableEventException("Admin action event without event or idempotency key");
        }
        auditLogService.record(event);
    }
}
