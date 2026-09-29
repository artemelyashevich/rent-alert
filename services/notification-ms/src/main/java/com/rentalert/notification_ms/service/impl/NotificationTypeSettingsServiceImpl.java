package com.rentalert.notification_ms.service.impl;

import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.domain.NotificationTypeSetting;
import com.rentalert.notification_ms.repository.NotificationTypeSettingRepository;
import com.rentalert.notification_ms.service.NotificationTypeSettings;
import com.rentalert.notification_ms.service.NotificationTypeSettingsService;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Keeps notification_type_settings in memory: it is read for every notification and changes rarely.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTypeSettingsServiceImpl implements NotificationTypeSettingsService, SmartInitializingSingleton {

    private final NotificationTypeSettingRepository repository;

    private volatile Map<NotificationType, NotificationTypeSettings> settings = Map.of();

    // A type added to the enum without a settings row would silently never be delivered: refuse to start
    @Override
    public void afterSingletonsInstantiated() {
        reload();
        List<NotificationType> missing = Arrays.stream(NotificationType.values())
            .filter(type -> !settings.containsKey(type))
            .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("No notification_type_settings row for " + missing);
        }
    }

    @Override
    public NotificationTypeSettings get(NotificationType type) {
        NotificationTypeSettings typeSettings = settings.get(type);
        if (typeSettings == null) {
            throw new IllegalStateException("No notification_type_settings row for " + type);
        }
        return typeSettings;
    }

    @Override
    @Scheduled(fixedDelayString = "${app.notification.type-settings-refresh:PT5M}",
        initialDelayString = "${app.notification.type-settings-refresh:PT5M}")
    public void reload() {
        Map<NotificationType, NotificationTypeSettings> loaded = new EnumMap<>(NotificationType.class);
        for (NotificationTypeSetting row : repository.findAll()) {
            loaded.put(row.getNotificationType(), new NotificationTypeSettings(row.getNotificationType(),
                row.getCategory(), List.copyOf(row.getEngines()), row.isMandatory(), row.isEnabled()));
        }
        settings = Map.copyOf(loaded);
        log.debug("Loaded {} notification type settings", loaded.size());
    }
}
