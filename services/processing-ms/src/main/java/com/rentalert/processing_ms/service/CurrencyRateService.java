package com.rentalert.processing_ms.service;

import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.processing_ms.domain.CurrencyRate;

public interface CurrencyRateService {

    long syncCurrencyRates();

    CurrencyRate getLatestRate(CurrencyPair pair);
}
