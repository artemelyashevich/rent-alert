package com.rentalert.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.listing.FavoriteViewingDto;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.internal.client.service.impl.ListingMsServiceImpl;
import com.rentalert.internal.resilience.RetryableExceptionPredicate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ListingMsServiceImplTests {

    private final ListingMsClient client = mock(ListingMsClient.class);
    private final ListingMsService service = new ListingMsServiceImpl(
        client,
        RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1))
            .retryOnException(new RetryableExceptionPredicate())
            .build()),
        CircuitBreakerRegistry.ofDefaults()
    );

    @Test
    void retriesUnavailableListing() {
        FavoriteViewingDto viewing = new FavoriteViewingDto(42L, 15L, Instant.parse("2026-10-05T12:00:00Z"), null);
        when(client.getFavoriteViewing(42L, 15L))
            .thenThrow(new UpstreamHttpException("unavailable", 503, null))
            .thenReturn(viewing);

        assertThat(service.getFavoriteViewing(42L, 15L)).contains(viewing);
        verify(client, times(2)).getFavoriteViewing(42L, 15L);
    }

    @Test
    void missingFavoriteIsEmpty() {
        assertThat(service.getFavoriteViewing(42L, 15L)).isEmpty();
    }
}
