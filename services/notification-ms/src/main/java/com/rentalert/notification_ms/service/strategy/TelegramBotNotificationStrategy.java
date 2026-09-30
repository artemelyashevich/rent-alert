package com.rentalert.notification_ms.service.strategy;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.notification_ms.config.ApplicationProperties;
import com.rentalert.notification_ms.service.BotBlockedService;
import com.rentalert.notification_ms.service.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramBotNotificationStrategy implements NotificationStrategy {

    private final ApplicationProperties applicationProperties;
    private final TelegramSendMessageClient telegramSendMessageClient;
    private final BotBlockedService botBlockedService;

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        try {
            telegramSendMessageClient.send(applicationProperties.getTelegram().getBotToken(),
                notificationDto.getReceiver(), notificationDto.getContent());
        } catch (AppException e) {
            if (e.getHttpStatus() == HttpStatus.FORBIDDEN && notificationDto.getUserId() != null) {
                botBlockedService.handle(notificationDto.getUserId());
            }
            throw e;
        }
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.TELEGRAM_BOT;
    }
}
