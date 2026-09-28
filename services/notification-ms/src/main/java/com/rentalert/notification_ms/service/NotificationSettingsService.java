package com.rentalert.notification_ms.service;

import com.rentalert.dto.notification.NotificationChannelDto;
import com.rentalert.dto.notification.UpdateNotificationSettingsRequest;
import java.util.List;

public interface NotificationSettingsService {

    List<NotificationChannelDto> getSettings(Long userId);

    List<NotificationChannelDto> updateSettings(Long userId, UpdateNotificationSettingsRequest request);

    void createDefault(Long userId);
}