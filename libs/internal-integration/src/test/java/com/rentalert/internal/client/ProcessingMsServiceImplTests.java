package com.rentalert.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.internal.client.service.ProcessingMsService;
import com.rentalert.internal.client.service.impl.ProcessingMsServiceImpl;
import com.rentalert.internal.resilience.RetryableExceptionPredicate;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessingMsServiceImplTests {

    private final ProcessingMsClient client = mock(ProcessingMsClient.class);
    private final CacheService cacheService = mock(CacheService.class);
    private final ProcessingMsService service = new ProcessingMsServiceImpl(
        client,
        cacheService,
        RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1))
            .retryOnException(new RetryableExceptionPredicate())
            .build()),
        CircuitBreakerRegistry.ofDefaults()
    );

    private final CurrencyRateDto rate = new CurrencyRateDto(LocalDate.of(2026, 9, 30), "USD", "BYN", new BigDecimal("3.27"));

    @Test
    void usesCachedRate() {
        when(cacheService.get(CacheType.DAILY_CURRENCY_RATE, CurrencyPair.USD_BYN)).thenReturn(Optional.of(rate));

        assertThat(service.getLatestRate(CurrencyPair.USD_BYN)).contains(rate);
        verifyNoInteractions(client);
    }

    @Test
    void fetchesAndCachesMissingRate() {
        when(cacheService.get(CacheType.DAILY_CURRENCY_RATE, CurrencyPair.USD_BYN)).thenReturn(Optional.empty());
        when(client.getLatestRate(CurrencyPair.USD_BYN))
            .thenThrow(new UpstreamHttpException("unavailable", 503, null))
            .thenReturn(rate);

        assertThat(service.getLatestRate(CurrencyPair.USD_BYN)).contains(rate);
        verify(client, times(2)).getLatestRate(CurrencyPair.USD_BYN);
        verify(cacheService).put(eq(CacheType.DAILY_CURRENCY_RATE), eq(CurrencyPair.USD_BYN), eq(rate), any());
    }

    @Test
    void doesNotCacheAbsentRate() {
        when(cacheService.get(CacheType.DAILY_CURRENCY_RATE, CurrencyPair.USD_BYN)).thenReturn(Optional.empty());

        assertThat(service.getLatestRate(CurrencyPair.USD_BYN)).isEmpty();
        verify(cacheService, never()).put(any(), any(), any(), any());
    }
}
