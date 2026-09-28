package com.rentalert.dto.notification;

import java.time.Instant;

public record BroadcastSummaryResponse(
    Long id,
    String message,
    Long createdByUserId,
    String createdByUsername,
    int totalRecipients,
    int sent,
    int failed,
    int skipped,
    boolean pinned,
    Instant createdAt
) {
}
