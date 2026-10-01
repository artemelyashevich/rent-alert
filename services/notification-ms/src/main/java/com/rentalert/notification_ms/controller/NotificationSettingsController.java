package com.rentalert.notification_ms.controller;

import com.rentalert.dto.notification.NotificationChannelDto;
import com.rentalert.dto.notification.UpdateNotificationSettingsRequest;
import com.rentalert.notification_ms.config.PublicUrls;
import com.rentalert.notification_ms.service.NotificationSettingsService;
import com.rentalert.redis.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification settings", description = "Каналы уведомлений пользователя")
@RestController
@RequestMapping(PublicUrls.SETTINGS_TAG)
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;
    private final UserSessionService userSessionService;

    @Operation(summary = "Получить каналы уведомлений текущего пользователя")
    @GetMapping
    public List<NotificationChannelDto> getSettings() {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return notificationSettingsService.getSettings(userId);
    }

    @Operation(summary = "Обновить каналы уведомлений текущего пользователя")
    @PutMapping
    public List<NotificationChannelDto> updateSettings(@Valid @RequestBody UpdateNotificationSettingsRequest request) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return notificationSettingsService.updateSettings(userId, request);
    }
}
