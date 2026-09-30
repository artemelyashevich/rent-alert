package com.rentalert.notification_ms.service.impl;

import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import com.rentalert.notification_ms.service.BotBlockedService;
import com.rentalert.notification_ms.service.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BotBlockedServiceImpl implements BotBlockedService {

    private final UserEventPublisher userEventPublisher;
    private final NotificationService notificationService;
    private final ZoneId zone;

    public BotBlockedServiceImpl(UserEventPublisher userEventPublisher, @Lazy NotificationService notificationService, @Value("${app.bot.timezone:Europe/Minsk}") ZoneId zone) {
        this.userEventPublisher = userEventPublisher;
        this.notificationService = notificationService;
        this.zone = zone;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(Long userId) {
        userEventPublisher.publishBotBlocked(userId);
        notificationService.enqueue(NotificationRequest.builder()
            .producer("notification-ms")
            .userId(userId)
            .type(NotificationType.BOT_BLOCKED)
            .templateParams(Map.of("message", String.valueOf(userId)))
            .deduplicationKey("bot-blocked:%d:%s".formatted(userId, LocalDate.now(zone)))
            .build(), Instant.now());
    }
}
