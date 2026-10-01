package com.rentalert.internal.client.service.impl;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.dto.listing.CityRequest;
import com.rentalert.dto.listing.CityResponse;
import com.rentalert.dto.listing.CityUpdateRequest;
import com.rentalert.dto.listing.SchedulerSettingsRequest;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.dto.statistics.ListingStatisticsOverview;
import com.rentalert.internal.client.AbstractResilientClient;
import com.rentalert.internal.client.ProcessingMsClient;
import com.rentalert.internal.client.service.ProcessingMsService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

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

    @Override
    public PageResponse<CityResponse> findCities(Pageable pageable) {
        return execute(() -> processingMsClient.findCities(pageable.getPageNumber(), pageable.getPageSize(),
            PageUtil.toSortParams(pageable)));
    }

    @Override
    public CityResponse findCity(Long id) {
        return execute(() -> processingMsClient.findCity(id));
    }

    @Override
    public CityResponse createCity(CityRequest request) {
        return executeOnce(() -> processingMsClient.createCity(request));
    }

    @Override
    public CityResponse updateCity(Long id, CityUpdateRequest request) {
        return executeOnce(() -> processingMsClient.updateCity(id, request));
    }

    @Override
    public CityResponse setCityActive(Long id, boolean active) {
        return executeOnce(() -> processingMsClient.setCityActive(id, active));
    }

    @Override
    public void deleteCity(Long id) {
        executeOnce(() -> processingMsClient.deleteCity(id));
    }

    @Override
    public List<SchedulerSettingsResponse> findSchedulerSettings() {
        return execute(() -> processingMsClient.findSchedulerSettings());
    }

    @Override
    public SchedulerSettingsResponse findSchedulerSettings(String providerName) {
        return execute(() -> processingMsClient.findSchedulerSettings(providerName));
    }

    @Override
    public SchedulerSettingsResponse createSchedulerSettings(SchedulerSettingsRequest request) {
        return executeOnce(() -> processingMsClient.createSchedulerSettings(request));
    }

    @Override
    public SchedulerSettingsResponse updateSchedulerSettings(SchedulerSettingsRequest request) {
        return executeOnce(() -> processingMsClient.updateSchedulerSettings(request));
    }

    @Override
    public void deleteSchedulerSettings(String providerName) {
        executeOnce(() -> processingMsClient.deleteSchedulerSettings(providerName));
    }

    @Override
    public void syncCurrencyRates() {
        executeOnce(processingMsClient::syncCurrencyRates);
    }

    @Override
    public ListingStatisticsOverview getListingStatistics(LocalDate since) {
        return execute(() -> processingMsClient.getListingStatistics(since.toString()));
    }
}
