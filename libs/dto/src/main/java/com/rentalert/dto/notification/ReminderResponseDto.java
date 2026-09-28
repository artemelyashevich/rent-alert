package com.rentalert.dto.notification;

import com.bsu.renalert.notification.domain.ReminderStatus;

import java.time.Instant;

public record ReminderResponseDto(
        Long id,
        Long listingId,
        ReminderIntervalType intervalType,
        Instant remindAt,
        ReminderStatus status,
        Instant sentAt,
        Instant createdAt
) {
}
