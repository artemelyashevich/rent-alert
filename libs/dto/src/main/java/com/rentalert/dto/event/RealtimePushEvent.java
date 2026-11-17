package com.rentalert.dto.event;

import com.rentalert.dto.notification.RealtimeNotificationPayload;
import java.util.UUID;

/**
 * Realtime push for one user, sent by notification-ms to {@link KafkaTopics#REALTIME_PUSH} and delivered over WebSocket by realtime-ms.
 * {@code userId} is the STOMP principal name; {@code eventId} lets consumers drop redelivered duplicates.
 */
public record RealtimePushEvent(UUID eventId, String userId, RealtimeNotificationPayload payload) {

}
