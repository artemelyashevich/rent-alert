package com.rentalert.dto.notification;

import com.rentalert.dto.ValidationConstants;
import jakarta.validation.constraints.NotNull;

public record ReminderRequestDto(
    @NotNull(message = ValidationConstants.NOT_NULL) ReminderIntervalType intervalType,
    NotificationEngine engine,
    @NotNull(message = ValidationConstants.NOT_NULL) Long listingId
) {

}
