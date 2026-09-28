package com.rentalert.dto.notification;

import com.rentalert.dto.event.KafkaTopics;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

/**
 * A request to notify a user, sent to {@link KafkaTopics#notificationRequests(NotificationType)} with key = userId.
 * Notification picks the channels, renders the template for {@code type} and delivers.
 *
 * @param requestId        idempotency of the Kafka delivery: a redelivered request is ignored
 * @param deduplicationKey business deduplication, e.g. {@code listing:123:user:42}: sent once per channel
 * @param expiresAt        a request still undelivered after this moment is dropped (email codes)
 */
@Builder
public record NotificationRequest(
    UUID requestId,
    Instant occurredAt,
    String producer,
    Long userId,
    NotificationType type,
    Map<String, String> templateParams,
    String deduplicationKey,
    Receiver receiver,
    Long listingId,
    Instant expiresAt
) {

    public NotificationRequest {
        requestId = requestId == null ? UUID.randomUUID() : requestId;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    /**
     * Addresses at the time of the request, encrypted with the shared {@code app.crypto.aes-key}.
     * Not needed for admin notifications: the admin chat comes from notification's own settings.
     */
    public record Receiver(String telegramIdEnc, String emailEnc) {
    }
}
