package com.rentalert.notification_ms.service.impl;

import static com.rentalert.common.exception.InternalExceptionCode.ERROR_NOTIFICATION_DEFAULT_ENGINE_NOT_ENABLED;
import static com.rentalert.common.exception.InternalExceptionCode.ERROR_NOTIFICATION_DEFAULT_ENGINE_UNRESOLVED;
import static com.rentalert.common.exception.InternalExceptionCode.ERROR_NOTIFICATION_ENGINE_ADMIN_ONLY;
import static com.rentalert.common.exception.InternalExceptionCode.ERROR_NOTIFICATION_ENGINE_UNAVAILABLE;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.notification.NotificationChannelDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.UpdateNotificationSettingsRequest;
import com.rentalert.notification_ms.domain.UserNotificationSetting;
import com.rentalert.notification_ms.mapper.NotificationChannelMapper;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import com.rentalert.notification_ms.repository.UserNotificationSettingRepository;
import com.rentalert.notification_ms.service.NotificationSettingsService;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingsServiceImpl implements NotificationSettingsService {

    private final UserNotificationSettingRepository userNotificationRepository;
    private final NotificationChannelMapper notificationChannelMapper;
    private final UserEventPublisher userEventPublisher;

    @Override
    public List<NotificationChannelDto> getSettings(Long userId) {
        List<UserNotificationSetting> stored = userNotificationRepository.findByUserId(userId);
        Map<NotificationEngine, UserNotificationSetting> existing = (stored.isEmpty() ? List.of(defaultSetting(userId)) : stored).stream()
            .collect(Collectors.toMap(UserNotificationSetting::getEngine, s -> s));
        return userSelectableEngines()
            .map(engine -> notificationChannelMapper.toDto(engine, existing.get(engine)))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<NotificationChannelDto> updateSettings(Long userId, UpdateNotificationSettingsRequest request) {
        List<NotificationEngine> requestedEngines = request.getEnabledEngines() == null
            ? List.of()
            : request.getEnabledEngines();
        LinkedHashSet<NotificationEngine> enabledEngines = new LinkedHashSet<>(requestedEngines);
        for (NotificationEngine engine : enabledEngines) {
            if (engine.isForAdmin()) {
                throw new AppException(ERROR_NOTIFICATION_ENGINE_ADMIN_ONLY, HttpStatus.BAD_REQUEST);
            }
            if (!engine.isEnabled()) {
                throw new AppException(ERROR_NOTIFICATION_ENGINE_UNAVAILABLE, HttpStatus.BAD_REQUEST);
            }
        }

        userNotificationRepository.deleteByUserId(userId);

        // A missing row is treated as "enabled" by the send path (see NotificationServiceImpl),
        // so disabling every channel must persist explicit isEnabled=false rows rather than
        // just deleting them - otherwise the user would silently go back to receiving everything.
        List<UserNotificationSetting> toSave;
        if (enabledEngines.isEmpty()) {
            toSave = userSelectableEngines()
                .map(engine -> UserNotificationSetting.builder()
                    .userId(userId)
                    .engine(engine)
                    .isEnabled(false)
                    .isDefault(false)
                    .build())
                .collect(Collectors.toList());
        } else {
            NotificationEngine defaultEngine = resolveDefaultEngine(enabledEngines, request.getDefaultEngine());
            toSave = enabledEngines.stream()
                .map(engine -> UserNotificationSetting.builder()
                    .userId(userId)
                    .engine(engine)
                    .isEnabled(true)
                    .isDefault(engine == defaultEngine)
                    .build())
                .collect(Collectors.toList());
        }
        userNotificationRepository.saveAll(toSave);
        userEventPublisher.publishSettingsUpdated(userId);
        Map<NotificationEngine, UserNotificationSetting> saved = toSave.stream()
            .collect(Collectors.toMap(UserNotificationSetting::getEngine, s -> s));

        return userSelectableEngines()
            .map(engine -> notificationChannelMapper.toDto(engine, saved.get(engine)))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createDefault(Long userId) {
        if (!userNotificationRepository.findByUserId(userId).isEmpty()) {
            return;
        }
        userNotificationRepository.save(defaultSetting(userId));
    }

    private UserNotificationSetting defaultSetting(Long userId) {
        return UserNotificationSetting.builder()
            .userId(userId)
            .engine(NotificationEngine.TELEGRAM_BOT)
            .isEnabled(true)
            .isDefault(true)
            .build();
    }

    private NotificationEngine resolveDefaultEngine(Set<NotificationEngine> enabledEngines, NotificationEngine requestedDefault) {
        if (requestedDefault != null) {
            if (!enabledEngines.contains(requestedDefault)) {
                throw new AppException(ERROR_NOTIFICATION_DEFAULT_ENGINE_NOT_ENABLED, HttpStatus.BAD_REQUEST);
            }
            return requestedDefault;
        }

        return Arrays.stream(NotificationEngine.values())
            .filter(enabledEngines::contains)
            .findFirst()
            .orElseThrow(() -> new AppException(ERROR_NOTIFICATION_DEFAULT_ENGINE_UNRESOLVED, HttpStatus.BAD_REQUEST));
    }

    private Stream<NotificationEngine> userSelectableEngines() {
        return Arrays.stream(NotificationEngine.values())
            .filter(e -> !e.isForAdmin() && e != NotificationEngine.WEBSOCKET);
    }
}