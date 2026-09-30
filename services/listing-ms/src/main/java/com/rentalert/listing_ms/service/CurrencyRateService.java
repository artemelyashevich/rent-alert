package com.rentalert.listing_ms.service;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.listing_ms.domain.CurrencyRate;
import com.rentalert.listing_ms.dto.CurrencyRateResponse;

import java.math.BigDecimal;
import java.util.Optional;

public interface CurrencyRateService {

    CurrencyRate getLatestRate(CurrencyPair pair);

    Optional<BigDecimal> convert(BigDecimal price, Currency currency, Currency currency2);

    CurrencyRateResponse getAllLatestRates();
}
