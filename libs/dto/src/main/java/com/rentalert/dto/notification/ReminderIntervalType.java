package com.rentalert.dto.notification;

import java.time.Duration;
import java.time.Instant;

public enum ReminderIntervalType {
    BEFORE_30_MIN(Duration.ofMinutes(30)),
    BEFORE_1_HOUR(Duration.ofHours(1)),
    BEFORE_1_DAY(Duration.ofDays(1));

    private final Duration offset;

    ReminderIntervalType(Duration offset) {
        this.offset = offset;
    }

    public Instant applyTo(Instant viewingDate) {
        return viewingDate.minus(offset);
    }
}
