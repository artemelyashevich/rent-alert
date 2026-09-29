package com.rentalert.notification_ms.service;

import com.rentalert.dto.notification.NotificationType;

public interface NotificationTypeSettingsService {

    NotificationTypeSettings get(NotificationType type);

    /**
     * Rereads the table, so edits apply without a restart.
     */
    void reload();
}
