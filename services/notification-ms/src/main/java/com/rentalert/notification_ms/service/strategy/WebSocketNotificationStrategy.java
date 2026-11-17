package com.rentalert.notification_ms.service.strategy;

import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.RealtimeNotificationPayload;
import com.rentalert.notification_ms.messaging.RealtimePushPublisher;
import com.rentalert.notification_ms.service.NotificationStrategy;
import com.rentalert.notification_ms.service.NotificationTypeSettingsService;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationStrategy implements NotificationStrategy {

    private final NotificationTypeSettingsService typeSettingsService;
    private final RealtimePushPublisher realtimePushPublisher;
    private final TransactionTemplate transactionTemplate;
    @Value("${ws.enabled:false}")
    private boolean enabled;

    @Override
    public void sendNotification(NotificationDto dto) {
        if (!enabled) {
            return;
        }

        if (dto.getUserId() == null) {
            log.warn("WS notification without userId. type={}", dto.getType());
            return;
        }
        String userId = String.valueOf(dto.getUserId());

        Map<String, String> params = dto.getTemplateParams() == null
            ? Map.of()
            : Map.copyOf(dto.getTemplateParams());

        RealtimeNotificationPayload payload = new RealtimeNotificationPayload(
            dto.getId(),
            dto.getType(),
            typeSettingsService.get(dto.getType()).category(),
            dto.getListingId(),
            dto.getContent(),
            params,
            Instant.now()
        );

        transactionTemplate.executeWithoutResult(status ->
            realtimePushPublisher.publishPush(userId, payload));
        log.debug("WS notification queued for realtime push. userId={}, type={}", userId, dto.getType());
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.WEBSOCKET;
    }
}
