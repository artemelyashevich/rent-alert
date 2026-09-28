package com.rentalert.dto.notification;

import java.time.Instant;

public record BroadcastRecipientResponse(
    Long id,
    Long userId,
    String username,
    String photoUrl,
    BroadcastRecipientStatus status,
    String errorMessage,
    Instant createdAt
) {
}
