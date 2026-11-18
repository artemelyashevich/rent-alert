package com.rentalert.roommate_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.notification.NotificationDelivery;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoommateNotificationPublisher extends AbstractOutboxEventPublisher {

    private static final String PRODUCER = "roommate-ms";

    private final IdentityMsService identityMsService;

    public void publish(Long userId, NotificationType type, String message, String deduplicationKey) {
        publish(userId, type, Map.of("message", message), deduplicationKey, null);
    }

    public void publish(
        Long userId, NotificationType type, Map<String, String> templateParams, String deduplicationKey, Instant notBefore) {
        NotificationDelivery delivery;
        try {
            delivery = identityMsService.getDelivery(userId);
        } catch (RuntimeException e) {
            log.warn("Skipping {} for user {}: delivery lookup failed: {}", type, userId, e.getMessage());
            return;
        }
        NotificationRequest request = NotificationRequest.builder()
            .producer(PRODUCER)
            .userId(userId)
            .type(type)
            .templateParams(templateParams)
            .deduplicationKey(deduplicationKey)
            .receiver(delivery.receiver())
            .sendAt(later(delivery.sendAt(), notBefore))
            .build();
        publish(KafkaTopics.notificationRequests(type), String.valueOf(userId), request);
    }

    private static Instant later(Instant a, Instant b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }
}
