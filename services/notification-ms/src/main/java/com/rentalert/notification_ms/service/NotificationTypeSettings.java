package com.rentalert.notification_ms.service;

import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import java.util.List;

/**
 * Delivery settings of a notification type (cached copy of a notification_type_settings row).
 */
public record NotificationTypeSettings(
    NotificationType type,
    String category,
    List<NotificationEngine> engines,
    boolean mandatory,
    boolean enabled
) {
}
