package com.rentalert.notification_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.ServiceStartedEvent;
import com.rentalert.notification_ms.service.AppReadinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceStartedListener {

    private final AppReadinessService appReadinessService;

    /**
     * Never fails: the topic has no dead-letter topic, and a lost start report only costs the "application started"
     * notification.
     */
    @KafkaListener(topics = KafkaTopics.SERVICE_STARTED, groupId = "notification.service-started")
    public void onServiceStarted(ServiceStartedEvent event) {
        try {
            appReadinessService.markStarted(event);
        } catch (Exception e) {
            log.warn("Failed to record start of {}: {}", event.service(), e.getMessage());
        }
    }
}
