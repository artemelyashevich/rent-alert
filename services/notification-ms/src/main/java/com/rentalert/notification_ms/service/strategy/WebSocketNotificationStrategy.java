package com.rentalert.notification_ms.service.strategy;

import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.RealtimeNotificationPayload;
import com.rentalert.notification_ms.service.NotificationStrategy;
import com.rentalert.notification_ms.service.NotificationTypeSettingsService;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationStrategy implements NotificationStrategy {

    static final String USER_DESTINATION = "/queue/notifications";
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private final NotificationTypeSettingsService typeSettingsService;
    @Value("${ws.enabled:false}")
    private boolean enabled;

    @Override
    public void sendNotification(NotificationDto dto) {
        if (!enabled) {
            return;
        }
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate == null) {
            log.debug("WebSocket disabled, dropping realtime notification. type={}", dto.getType());
            return;
        }

        String userId = dto.getReceiver();
        if (userId == null || userId.isBlank()) {
            log.warn("WS notification without receiver (userId). type={}", dto.getType());
            return;
        }

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

        messagingTemplate.convertAndSendToUser(userId, USER_DESTINATION, payload);
        log.debug("WS notification pushed. userId={}, type={}", userId, dto.getType());
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.WEBSOCKET;
    }
}
