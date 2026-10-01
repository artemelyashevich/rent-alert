package com.rentalert.notification_ms.service.strategy;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.notification_ms.bot.ListingActionsKeyboardFactory;
import com.rentalert.notification_ms.config.ApplicationProperties;
import com.rentalert.notification_ms.service.BotBlockedService;
import com.rentalert.notification_ms.service.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Service
@RequiredArgsConstructor
public class TelegramBotNotificationStrategy implements NotificationStrategy {

    private final ApplicationProperties applicationProperties;
    private final TelegramSendMessageClient telegramSendMessageClient;
    private final BotBlockedService botBlockedService;
    private final ListingActionsKeyboardFactory keyboardFactory;

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        try {
            String botToken = applicationProperties.getTelegram().getBotToken();
            InlineKeyboardMarkup replyMarkup = listingActions(notificationDto);
            if (notificationDto.isPin()) {
                telegramSendMessageClient.sendAndPin(botToken, notificationDto.getReceiver(), notificationDto.getContent(), replyMarkup);
            } else {
                telegramSendMessageClient.send(botToken, notificationDto.getReceiver(), notificationDto.getContent(), replyMarkup);
            }
        } catch (AppException e) {
            if (e.getHttpStatus() == HttpStatus.FORBIDDEN && notificationDto.getUserId() != null) {
                botBlockedService.handle(notificationDto.getUserId());
            }
            throw e;
        }
    }

    private InlineKeyboardMarkup listingActions(NotificationDto notificationDto) {
        if (notificationDto.getListingId() == null || !applicationProperties.getTelegram().isBotEnabled()) {
            return null;
        }
        return keyboardFactory.build(notificationDto.getListingId(), false);
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.TELEGRAM_BOT;
    }
}
