package com.rentalert.processing_ms.service.impl;

import com.rentalert.common.service.RestService;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.processing_ms.config.ApplicationProperties;
import com.rentalert.processing_ms.domain.CurrencyRate;
import com.rentalert.processing_ms.dto.FrankfurterRateResponse;
import com.rentalert.processing_ms.repository.CurrencyRateRepository;
import com.rentalert.processing_ms.service.CurrencyRateService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private static final Duration RATE_CACHE_TTL = Duration.ofHours(24);

    private final RestService restService;
    private final CurrencyRateRepository currencyRateRepository;
    private final ApplicationProperties applicationProperties;
    private final CacheService cacheService;

    private static CurrencyRateDto toDto(CurrencyRate rate) {
        return new CurrencyRateDto(rate.getRateDate(), rate.getCurrency(), rate.getPerCurrency(), rate.getRate());
    }

    private static CurrencyRate toEntity(CurrencyRateDto dto) {
        return CurrencyRate.builder()
            .rateDate(dto.rateDate())
            .currency(dto.currency())
            .perCurrency(dto.perCurrency())
            .rate(dto.rate())
            .build();
    }

    @Override
    public long syncCurrencyRates() {
        LocalDate today = LocalDate.now();
        String baseUrl = applicationProperties.getCurrency().getFrankfurterUrl();
        long count = 0;

        for (CurrencyPair pair : CurrencyPair.values()) {
            try {
                boolean alreadyExists = currencyRateRepository.existsByRateDateAndCurrencyAndPerCurrency(
                    today, pair.getFrom(), pair.getTo());

                if (alreadyExists) {
                    log.info("Rate already exists for {} -> {} on {}, skipping", pair.getFrom(), pair.getTo(), today);
                    continue;
                }

                String uri = buildRateUri(baseUrl, today, pair.getFrom(), pair.getTo());
                FrankfurterRateResponse resp = restService.processGetRequest(uri, FrankfurterRateResponse.class);

                CurrencyRate rate = CurrencyRate.builder()
                    .rateDate(LocalDate.parse(resp.date()))
                    .currency(resp.base())
                    .perCurrency(resp.quote())
                    .rate(resp.rate())
                    .build();

                currencyRateRepository.save(rate);
                log.info("Saved currency rate: {} 1{} = {} {}", resp.base(), resp.quote(), resp.rate(), today);
                cacheService.put(CacheType.DAILY_CURRENCY_RATE, pair, toDto(rate), RATE_CACHE_TTL);
                count++;
            } catch (Exception e) {
                log.error("Failed to fetch rate for {} -> {}: {}", pair.getFrom(), pair.getTo(), e.getMessage());
            }
        }

        log.info("Currency rates sync completed for {}", today);
        return count;
    }

    @Override
    public CurrencyRate getLatestRate(CurrencyPair currencyPair) {
        Optional<CurrencyRateDto> cached = cacheService.get(CacheType.DAILY_CURRENCY_RATE, currencyPair);
        if (cached.isPresent()) {
            return toEntity(cached.get());
        }
        CurrencyRate latest = currencyRateRepository.findLatestByCurrencyAndPerCurrency(currencyPair.getFrom(), currencyPair.getTo());
        if (latest == null) {
            log.warn("No currency rate found in DB for {} -> {}, not caching", currencyPair.getFrom(), currencyPair.getTo());
            return null;
        }
        cacheService.put(CacheType.DAILY_CURRENCY_RATE, currencyPair, toDto(latest), RATE_CACHE_TTL);
        return latest;
    }

    private String buildRateUri(String baseUrl, LocalDate date, String from, String to) {
        return UriComponentsBuilder.fromUriString(baseUrl)
            .pathSegment("v2", "rate", from, to)
            .queryParam("date", date.toString())
            .toUriString();
    }
}
