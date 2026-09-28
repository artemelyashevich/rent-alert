package com.rentalert.dto.notification;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationSettingsRequest {

    /** Пустой или null список означает полное отключение уведомлений пользователя. */
    private List<NotificationEngine> enabledEngines;

    private NotificationEngine defaultEngine;
}