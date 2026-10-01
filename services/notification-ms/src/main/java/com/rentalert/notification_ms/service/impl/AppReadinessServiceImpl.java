package com.rentalert.notification_ms.service.impl;

import com.rentalert.dto.event.ServiceStartedEvent;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.config.ApplicationProperties;
import com.rentalert.notification_ms.service.AppReadinessService;
import com.rentalert.notification_ms.service.NotificationService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppReadinessServiceImpl implements AppReadinessService {

    static final String STARTED_SERVICES_KEY = "app:started-services";
    static final Duration STARTUP_WINDOW = Duration.ofMinutes(15);

    private final ApplicationProperties applicationProperties;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void markStarted(ServiceStartedEvent event) {
        if (event.startedAt() == null || event.startedAt().isBefore(Instant.now().minus(STARTUP_WINDOW))) {
            log.debug("Ignoring stale start report of {}", event.service());
            return;
        }

        HashOperations<String, String, String> hash = redisTemplate.opsForHash();
        hash.put(STARTED_SERVICES_KEY, event.service(), event.host());
        redisTemplate.expire(STARTED_SERVICES_KEY, STARTUP_WINDOW);

        Map<String, String> started = hash.entries(STARTED_SERVICES_KEY);
        List<String> required = applicationProperties.getStartup().getServices();
        if (!started.keySet().containsAll(required)) {
            log.info("Service {} started on {}, waiting for {}", event.service(), event.host(),
                required.stream().filter(s -> !started.containsKey(s)).toList());
            return;
        }
        if (!Boolean.TRUE.equals(redisTemplate.delete(STARTED_SERVICES_KEY))) {
            return;
        }

        String hosts = required.stream()
            .map(service -> "%s (%s)".formatted(service, started.get(service)))
            .collect(Collectors.joining(", "));
        notificationService.enqueue(NotificationRequest.builder()
            .producer("notification-ms")
            .type(NotificationType.APP_STARTED)
            .templateParams(Map.of("message", "Приложение готово к работе: " + hosts))
            .build(), Instant.now());
        log.info("All services started, admin notified");
    }
}
