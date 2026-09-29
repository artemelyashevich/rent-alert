package com.rentalert.listing_ms.service.impl;

import com.rentalert.listing_ms.domain.CurrencyRate;
import com.rentalert.listing_ms.dto.CurrencyRateResponse;
import com.rentalert.listing_ms.repository.CurrencyRateRepository;
import com.rentalert.listing_ms.service.CurrencyRateService;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private static final List<CurrencyPair> BYN_QUOTED_PAIRS = List.of(CurrencyPair.USD_BYN, CurrencyPair.EUR_BYN);
    private static final Duration RATE_CACHE_TTL = Duration.ofHours(24);

    private final CurrencyRateRepository currencyRateRepository;
    private final CacheService cacheService;

    @Override
    @Transactional
    public void saveRate(CurrencyRateDto rate) {
        if (currencyRateRepository.existsByRateDateAndCurrencyAndPerCurrency(rate.rateDate(), rate.currency(), rate.perCurrency())) {
            log.debug("Rate {} -> {} on {} already stored, skipping", rate.currency(), rate.perCurrency(), rate.rateDate());
            return;
        }
        currencyRateRepository.save(toEntity(rate));
        log.info("Saved currency rate: {} 1{} = {} {}", rate.currency(), rate.perCurrency(), rate.rate(), rate.rateDate());
        Arrays.stream(CurrencyPair.values())
            .filter(pair -> pair.getFrom().equals(rate.currency()) && pair.getTo().equals(rate.perCurrency()))
            .findFirst()
            .ifPresent(pair -> cacheService.put(CacheType.DAILY_CURRENCY_RATE, pair, rate, RATE_CACHE_TTL));
    }

    @Override
    public CurrencyRate getLatestRate(CurrencyPair pair) {
        return findLatestAndCache(pair);
    }

    @Override
    public Optional<BigDecimal> convert(
            BigDecimal price,
            Currency from,
            Currency to
    ) {
        if (price == null || from == null || to == null) {
            return Optional.empty();
        }

        if (from == to) {
            return Optional.of(price.setScale(2, RoundingMode.HALF_UP));
        }

        CurrencyPair pair = CurrencyPair.getPair(from, to);

        if (pair == null) {
            log.warn("Currency pair not found for {} -> {}", from, to);
            return Optional.empty();
        }

        try {
            CurrencyRate rate = getLatestRate(pair);
            if (rate == null) {
                return Optional.empty();
            }

            BigDecimal converted = price
                    .multiply(rate.getRate())
                    .setScale(2, RoundingMode.HALF_UP);

            return Optional.of(converted);
        } catch (Exception e) {
            log.error(
                    "Failed to convert {} {} -> {}",
                    price,
                    from,
                    to,
                    e
            );

            return Optional.empty();
        }
    }

    private CurrencyRate findLatestAndCache(CurrencyPair currencyPair) {
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

    @Override
    public CurrencyRateResponse getAllLatestRates() {
        List<CurrencyRate> rates = BYN_QUOTED_PAIRS.stream()
                .map(this::getLatestRate)
                .toList();

        return CurrencyRateResponse.fromRates(rates);
    }

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
}
