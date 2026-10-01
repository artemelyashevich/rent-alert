package com.rentalert.admin_ms.audit;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.redis.service.UserSessionService;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuditPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserSessionService userSessionService;

    public void publish(AdminEvent adminEvent, Object... params) {
        AdminActionEvent event = AdminActionEvent.builder()
            .event(adminEvent)
            .params(params.length == 0 ? null : Arrays.stream(params).map(String::valueOf).toList())
            .build();
        event.setInitiatorId(userSessionService.getCurrentUser().getUserId());
        event.setEventStatus(EventStatus.SUCCESS);

        kafkaTemplate.send(KafkaTopics.AUDIT_EVENTS, String.valueOf(event.getInitiatorId()), event)
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to publish admin audit event {}: {}", adminEvent, error.getMessage(), error);
                }
            });
    }
}
