package com.rentalert.notification_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.common.util.auth.HmacBlindIndexService;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.domain.Notification;
import com.rentalert.notification_ms.domain.NotificationStatus;
import com.rentalert.notification_ms.domain.NotificationTemplate;
import com.rentalert.notification_ms.mapper.NotificationMapper;
import com.rentalert.notification_ms.repository.NotificationRepository;
import com.rentalert.notification_ms.repository.NotificationTemplateRepository;
import com.rentalert.notification_ms.repository.UserNotificationSettingRepository;
import com.rentalert.notification_ms.service.NotificationStrategy;
import com.rentalert.notification_ms.service.NotificationTypeSettingsService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class NotificationServiceImplDispatchTests {

    private final NotificationStrategy telegram = mock(NotificationStrategy.class);
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final NotificationTemplateRepository templates = mock(NotificationTemplateRepository.class);
    private final NotificationMapper mapper = mock(NotificationMapper.class);
    private NotificationServiceImpl service;

    private static Notification queued() {
        return Notification.builder()
            .engine(NotificationEngine.TELEGRAM_BOT)
            .retryType(NotificationType.NEW_LISTING_MATCHED)
            .status(NotificationStatus.QUEUED)
            .attempt(0)
            .build();
    }

    @BeforeEach
    void setUp() {
        when(telegram.getSupportedEngine()).thenReturn(NotificationEngine.TELEGRAM_BOT);
        service = new NotificationServiceImpl(List.of(telegram), notifications, templates,
            mock(UserNotificationSettingRepository.class), mapper, mock(CryptoService.class),
            mock(HmacBlindIndexService.class), mock(NotificationTypeSettingsService.class));
        when(templates.findByNotificationTypeAndEngine(NotificationType.NEW_LISTING_MATCHED, NotificationEngine.TELEGRAM_BOT))
            .thenReturn(Optional.of(NotificationTemplate.builder().templateText("{{message}}").build()));
        when(mapper.toDto(any())).thenReturn(new NotificationDto());
    }

    @Test
    void blockedBotIsNotRetried() {
        Notification notification = queued();
        doThrow(new AppException("[TELEGRAM]: forbidden", HttpStatus.FORBIDDEN)).when(telegram).sendNotification(any());

        service.dispatch(List.of(notification));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.EXHAUSTED);
    }

    @Test
    void otherFailuresAreRetried() {
        Notification notification = queued();
        doThrow(new AppException("[TELEGRAM]: bad gateway", HttpStatus.BAD_GATEWAY)).when(telegram).sendNotification(any());

        service.dispatch(List.of(notification));

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILURE);
        assertThat(notification.getNextRetryAt()).isAfter(Instant.now());
    }
}
