package com.rentalert.notification_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.ServiceStartedEvent;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.notification_ms.service.AppReadinessService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceStartedListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "notification.service-started";

    private final AppReadinessService appReadinessService;

    public ServiceStartedListener(AppReadinessService appReadinessService) {
        super(CONSUMER);
        this.appReadinessService = appReadinessService;
    }

    /**
     * Never fails: the topic has no dead-letter topic, and a lost start report only costs the "application started"
     * notification.
     */
    @KafkaListener(topics = KafkaTopics.SERVICE_STARTED, groupId = CONSUMER)
    public void onServiceStarted(ServiceStartedEvent event) {
        try {
            processOnce(keyOf(event), () -> appReadinessService.markStarted(event));
        } catch (Exception e) {
            log.warn("Failed to record start of {}: {}", event.service(), e.getMessage());
        }
    }

    private static UUID keyOf(ServiceStartedEvent event) {
        return UUID.nameUUIDFromBytes(("ServiceStartedEvent:" + event.service() + ":" + event.host() + ":" + event.startedAt())
            .getBytes(StandardCharsets.UTF_8));
    }
}
