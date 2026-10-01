package com.rentalert.notification_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.notification.NotificationChannelDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.UpdateNotificationSettingsRequest;
import com.rentalert.notification_ms.domain.UserNotificationSetting;
import com.rentalert.notification_ms.mapper.NotificationChannelMapperImpl;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import com.rentalert.notification_ms.repository.UserNotificationSettingRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationSettingsServiceImplTests {

    private final UserNotificationSettingRepository repository = mock(UserNotificationSettingRepository.class);
    private final UserEventPublisher userEventPublisher = mock(UserEventPublisher.class);
    private final NotificationSettingsServiceImpl service = new NotificationSettingsServiceImpl(repository, new NotificationChannelMapperImpl(), userEventPublisher);

    @Test
    void userWithoutSettingsGetsTelegramByDefault() {
        when(repository.findByUserId(42L)).thenReturn(List.of());

        assertThat(service.getSettings(42L))
            .filteredOn(NotificationChannelDto::isEnabled)
            .singleElement()
            .satisfies(channel -> {
                assertThat(channel.getEngine()).isEqualTo(NotificationEngine.TELEGRAM_BOT);
                assertThat(channel.isDefault()).isTrue();
            });
    }

    @Test
    void storedSettingsReplaceDefault() {
        when(repository.findByUserId(42L)).thenReturn(List.of(UserNotificationSetting.builder()
            .userId(42L)
            .engine(NotificationEngine.EMAIL)
            .isEnabled(true)
            .isDefault(true)
            .build()));

        assertThat(service.getSettings(42L))
            .filteredOn(NotificationChannelDto::isEnabled)
            .extracting(NotificationChannelDto::getEngine)
            .containsExactly(NotificationEngine.EMAIL);
    }

    @Test
    void defaultSettingsAreCreatedOnce() {
        when(repository.findByUserId(42L)).thenReturn(List.of()).thenReturn(List.of(UserNotificationSetting.builder()
            .userId(42L)
            .engine(NotificationEngine.TELEGRAM_BOT)
            .build()));

        service.createDefault(42L);
        service.createDefault(42L);

        verify(repository, times(1)).save(any());
    }

    @Test
    void settingsUpdateIsSentToStatistics() {
        UpdateNotificationSettingsRequest request = new UpdateNotificationSettingsRequest();
        request.setEnabledEngines(List.of(NotificationEngine.TELEGRAM_BOT));

        service.updateSettings(42L, request);

        verify(userEventPublisher).publishSettingsUpdated(42L);
    }
}
