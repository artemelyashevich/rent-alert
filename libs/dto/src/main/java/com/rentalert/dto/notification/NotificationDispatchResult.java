package com.rentalert.dto.notification;

public record NotificationDispatchResult(
    NotificationEngine engine,
    NotificationDispatchStatus status,
    String reason
) {

}
