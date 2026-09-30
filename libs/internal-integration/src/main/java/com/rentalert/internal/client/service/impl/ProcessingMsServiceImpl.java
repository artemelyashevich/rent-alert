package com.rentalert.internal.client.service.impl;

import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.internal.client.AbstractResilientClient;
import com.rentalert.internal.client.ProcessingMsClient;
import com.rentalert.internal.client.service.ProcessingMsService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.Optional;

public class ProcessingMsServiceImpl extends AbstractResilientClient implements ProcessingMsService {

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final ProcessingMsClient processingMsClient;
    private final CacheService cacheService;

    public ProcessingMsServiceImpl(ProcessingMsClient processingMsClient, CacheService cacheService,
        RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        super(ProcessingMsClient.SERVICE_NAME, retryRegistry, circuitBreakerRegistry);
        this.processingMsClient = processingMsClient;
        this.cacheService = cacheService;
    }

    @Override
    public Optional<CurrencyRateDto> getLatestRate(CurrencyPair pair) {
        Optional<CurrencyRateDto> cached = cacheService.get(CacheType.DAILY_CURRENCY_RATE, pair);
        if (cached.isPresent()) {
            return cached;
        }
        CurrencyRateDto rate = execute(() -> processingMsClient.getLatestRate(pair));
        if (rate != null) {
            cacheService.put(CacheType.DAILY_CURRENCY_RATE, pair, rate, CACHE_TTL);
        }
        return Optional.ofNullable(rate);
    }
}
