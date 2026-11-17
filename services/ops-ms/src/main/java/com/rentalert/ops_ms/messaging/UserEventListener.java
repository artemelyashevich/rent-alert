package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.ops_ms.service.AuditLogService;
import com.rentalert.ops_ms.service.StatisticsService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "ops.user-events";

    private final AuditLogService auditLogService;
    private final StatisticsService statisticsService;

    public UserEventListener(AuditLogService auditLogService, StatisticsService statisticsService) {
        super(CONSUMER);
        this.auditLogService = auditLogService;
        this.statisticsService = statisticsService;
    }

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = CONSUMER)
    public void onUserAction(UserActionEvent event) {
        if (event.getEvent() == null || event.getIdempotencyKey() == null) {
            throw new NonRetryableEventException("User action event without event or idempotency key");
        }
        processOnce(event.getIdempotencyKey(), () -> handle(event));
    }

    private void handle(UserActionEvent event) {
        if (event.getEvent() != UserEvent.LISTING_RECEIVED) {
            auditLogService.recordUserAction(event);
        }
        if (event.getEvent() == UserEvent.USER_DELETED) {
            if (event.getInitiatorId() != null) {
                statisticsService.deleteUser(event.getInitiatorId());
            }
            return;
        }
        statisticsService.record(event);
    }
}
