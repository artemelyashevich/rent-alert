package com.rentalert.notification_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.ServiceStartedEvent;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.config.ApplicationProperties;
import com.rentalert.notification_ms.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class AppReadinessServiceImplTests {

    private static final String KEY = AppReadinessServiceImpl.STARTED_SERVICES_KEY;

    private final NotificationService notificationService = mock(NotificationService.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hash = mock(HashOperations.class);
    private AppReadinessServiceImpl service;

    private static ServiceStartedEvent started(String service) {
        return new ServiceStartedEvent(service, "host-" + service, Instant.now());
    }

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getStartup().setServices(List.of("identity-ms", "listing-ms"));
        service = new AppReadinessServiceImpl(properties, notificationService, redisTemplate);
        when(redisTemplate.opsForHash()).thenReturn(hash);
    }

    @Test
    void waitsUntilEveryServiceStarted() {
        when(hash.entries(KEY)).thenReturn(Map.of("identity-ms", "host-identity-ms"));

        service.markStarted(started("identity-ms"));

        verify(hash).put(KEY, "identity-ms", "host-identity-ms");
        verify(redisTemplate).expire(KEY, AppReadinessServiceImpl.STARTUP_WINDOW);
        verify(redisTemplate, never()).delete(KEY);
        verifyNoInteractions(notificationService);
    }

    @Test
    void notifiesAdminOnceAllServicesStarted() {
        when(hash.entries(KEY)).thenReturn(Map.of("identity-ms", "h1", "listing-ms", "h2"));
        when(redisTemplate.delete(KEY)).thenReturn(true);

        service.markStarted(started("listing-ms"));

        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).enqueue(request.capture(), any());
        assertThat(request.getValue().type()).isEqualTo(NotificationType.APP_STARTED);
        assertThat(request.getValue().templateParams().get("message"))
            .isEqualTo("Приложение готово к работе: identity-ms (h1), listing-ms (h2)");
    }

    @Test
    void anotherInstanceAlreadyNotified() {
        when(hash.entries(KEY)).thenReturn(Map.of("identity-ms", "h1", "listing-ms", "h2"));
        when(redisTemplate.delete(KEY)).thenReturn(false);

        service.markStarted(started("listing-ms"));

        verifyNoInteractions(notificationService);
    }

    @Test
    void staleReportIsIgnored() {
        service.markStarted(new ServiceStartedEvent("identity-ms", "h1", Instant.now().minus(AppReadinessServiceImpl.STARTUP_WINDOW).minusSeconds(1)));

        verifyNoInteractions(hash, notificationService);
    }
}
