package com.rentalert.notification_ms.service.strategy;

import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.notification_ms.config.ApplicationProperties;
import com.rentalert.notification_ms.service.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramAdminNotificationStrategy implements NotificationStrategy {

    private static final String MESSAGE_PREFIX = "[MICROSERVICE] ";

    private final ApplicationProperties applicationProperties;
    private final TelegramSendMessageClient telegramSendMessageClient;

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        ApplicationProperties.Telegram telegram = applicationProperties.getTelegram();
        telegramSendMessageClient.send(telegram.getAdminToken(), telegram.getAdminChatId(),
            MESSAGE_PREFIX + notificationDto.getContent());
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.TELEGRAM_ADMIN;
    }
}
