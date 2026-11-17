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

    /**
     * Queues a notification for the user through the outbox of the current transaction, deferred until the end of
     * the user's quiet hours. A failure to resolve the user's addresses is logged and the notification is skipped,
     * so it never rolls back the business change.
     */
    public void publish(Long userId, NotificationType type, String message, String deduplicationKey) {
        publish(userId, type, Map.of("message", message), deduplicationKey, null);
    }

    /**
     * Same as {@link #publish(Long, NotificationType, String, String)} with template parameters and an optional
     * {@code notBefore}; the later of {@code notBefore} and the end of quiet hours is used as the send time.
     */
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
