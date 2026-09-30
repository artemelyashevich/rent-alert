package com.rentalert.listing_ms.service.impl;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.internal.client.service.ProcessingMsService;
import com.rentalert.listing_ms.domain.CurrencyRate;
import com.rentalert.listing_ms.dto.CurrencyRateResponse;
import com.rentalert.listing_ms.service.CurrencyRateService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private static final List<CurrencyPair> BYN_QUOTED_PAIRS = List.of(CurrencyPair.USD_BYN, CurrencyPair.EUR_BYN);

    private final ProcessingMsService processingMsService;

    private static CurrencyRate toEntity(CurrencyRateDto dto) {
        return CurrencyRate.builder()
            .rateDate(dto.rateDate())
            .currency(dto.currency())
            .perCurrency(dto.perCurrency())
            .rate(dto.rate())
            .build();
    }

    @Override
    public CurrencyRate getLatestRate(CurrencyPair pair) {
        CurrencyRate rate = processingMsService.getLatestRate(pair).map(CurrencyRateServiceImpl::toEntity).orElse(null);
        if (rate == null) {
            log.warn("No currency rate in processing-ms for {} -> {}", pair.getFrom(), pair.getTo());
        }
        return rate;
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

    @Override
    public CurrencyRateResponse getAllLatestRates() {
        List<CurrencyRate> rates = BYN_QUOTED_PAIRS.stream()
            .map(this::getLatestRate)
            .toList();

        return CurrencyRateResponse.fromRates(rates);
    }
}
