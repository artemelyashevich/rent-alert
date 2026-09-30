package com.rentalert.notification_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.notification_ms.domain.Notification;
import com.rentalert.notification_ms.service.NotificationService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class NotificationRequestListener {

    private final NotificationService notificationService;
    private final Duration priorityDispatchGrace;

    public NotificationRequestListener(NotificationService notificationService, @Value("${app.notification.priority-dispatch-grace:PT30S}") Duration priorityDispatchGrace) {
        this.notificationService = notificationService;
        this.priorityDispatchGrace = priorityDispatchGrace;
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_REQUESTS, groupId = "notification.requests", concurrency = "${app.notification.requests-concurrency:3}")
    public void onRequest(NotificationRequest request) {
        if (isAcceptable(request)) {
            enqueue(request, deferredUntil(request).orElse(Instant.now()));
        }
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_REQUESTS_PRIORITY, groupId = "notification.requests.priority", concurrency = "${app.notification.priority-requests-concurrency:1}")
    public void onPriorityRequest(NotificationRequest request) {
        if (!isAcceptable(request)) {
            return;
        }
        Optional<Instant> deferredUntil = deferredUntil(request);
        if (deferredUntil.isPresent()) {
            enqueue(request, deferredUntil.get());
            return;
        }
        List<Notification> queued = enqueue(request, Instant.now().plus(priorityDispatchGrace));
        notificationService.dispatch(queued);
    }

    private boolean isAcceptable(NotificationRequest request) {
        if (request.requestId() == null || request.type() == null) {
            throw new NonRetryableEventException("Notification request without requestId or type");
        }
        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            log.warn("Dropping expired notification request {} of type {}, expired at {}",
                request.requestId(), request.type(), request.expiresAt());
            return false;
        }
        return true;
    }

    private static Optional<Instant> deferredUntil(NotificationRequest request) {
        return Optional.ofNullable(request.sendAt()).filter(sendAt -> sendAt.isAfter(Instant.now()));
    }

    private List<Notification> enqueue(NotificationRequest request, Instant sendAt) {
        try {
            return notificationService.enqueue(request, sendAt);
        } catch (IllegalArgumentException e) {
            throw new NonRetryableEventException("Invalid notification request %s: %s".formatted(request.requestId(), e.getMessage()));
        }
    }
}
