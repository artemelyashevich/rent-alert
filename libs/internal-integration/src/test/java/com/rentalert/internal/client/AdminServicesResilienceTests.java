package com.rentalert.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.listing.CityRequest;
import com.rentalert.dto.notification.BroadcastRequest;
import com.rentalert.internal.client.service.NotificationMsService;
import com.rentalert.internal.client.service.OpsMsService;
import com.rentalert.internal.client.service.ProcessingMsService;
import com.rentalert.internal.client.service.impl.NotificationMsServiceImpl;
import com.rentalert.internal.client.service.impl.OpsMsServiceImpl;
import com.rentalert.internal.client.service.impl.ProcessingMsServiceImpl;
import com.rentalert.internal.resilience.RetryableExceptionPredicate;
import com.rentalert.redis.service.CacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class AdminServicesResilienceTests {

    private static final UpstreamHttpException UNAVAILABLE = new UpstreamHttpException("unavailable", 503, null);

    private final RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
        .maxAttempts(3)
        .waitDuration(Duration.ofMillis(1))
        .retryOnException(new RetryableExceptionPredicate())
        .build());

    private final OpsMsClient opsMsClient = mock(OpsMsClient.class);
    private final OpsMsService opsMsService = new OpsMsServiceImpl(opsMsClient, retryRegistry, CircuitBreakerRegistry.ofDefaults());
    private final ProcessingMsClient processingMsClient = mock(ProcessingMsClient.class);
    private final ProcessingMsService processingMsService = new ProcessingMsServiceImpl(processingMsClient, mock(CacheService.class),
        retryRegistry, CircuitBreakerRegistry.ofDefaults());
    private final NotificationMsClient notificationMsClient = mock(NotificationMsClient.class);
    private final NotificationMsService notificationMsService = new NotificationMsServiceImpl(notificationMsClient, retryRegistry,
        CircuitBreakerRegistry.ofDefaults());

    @Test
    void readsAreRetriedWithPagingAndDatesEncoded() {
        PageResponse<Object> empty = new PageResponse<>(List.of(), 1, 5, 0, 0);
        when(opsMsClient.findJobSettings(1, 5, List.of("startedAt,DESC")))
            .thenThrow(UNAVAILABLE)
            .thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0));

        assertThat(opsMsService.findJobSettings(PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "startedAt"))))
            .isEqualTo(empty);
        verify(opsMsClient, times(2)).findJobSettings(1, 5, List.of("startedAt,DESC"));

        opsMsService.getSystemActivity(LocalDate.of(2026, 9, 1));
        verify(opsMsClient).getSystemActivity("2026-09-01");
    }

    @Test
    void cityCreationIsNotRetried() {
        when(processingMsClient.createCity(any())).thenThrow(UNAVAILABLE);

        assertThatThrownBy(() -> processingMsService.createCity(new CityRequest())).isSameAs(UNAVAILABLE);
        verify(processingMsClient).createCity(any());
    }

    @Test
    void broadcastIsNotRetried() {
        when(notificationMsClient.broadcast(any(), any())).thenThrow(UNAVAILABLE);

        assertThatThrownBy(() -> notificationMsService.broadcast(new BroadcastRequest(), 1L)).isSameAs(UNAVAILABLE);
        verify(notificationMsClient).broadcast(any(), any());
    }

    @Test
    void cleanupStartIsNotRetried() {
        when(opsMsClient.startCleanup()).thenThrow(UNAVAILABLE);

        assertThatThrownBy(opsMsService::startCleanup).isSameAs(UNAVAILABLE);
        verify(opsMsClient).startCleanup();
    }
}
