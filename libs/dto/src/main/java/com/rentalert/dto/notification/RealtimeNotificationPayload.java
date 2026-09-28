package com.rentalert.dto.notification;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record RealtimeNotificationPayload(
    String id,
    NotificationType type,
    String category,
    Long listingId,
    String body,
    Map<String, String> params,
    Instant createdAt
) {
}
