package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.ops_ms.service.AuditLogService;
import com.rentalert.ops_ms.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final AuditLogService auditLogService;
    private final StatisticsService statisticsService;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "ops.user-events")
    public void onUserAction(UserActionEvent event) {
        if (event.getEvent() == null || event.getIdempotencyKey() == null) {
            throw new NonRetryableEventException("User action event without event or idempotency key");
        }
        if (event.getEvent() != UserEvent.LISTING_RECEIVED) {
            auditLogService.recordUserAction(event);
        }
        statisticsService.record(event);
    }
}
