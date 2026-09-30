package com.rentalert.internal.client.service;

import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import java.util.Optional;

public interface ProcessingMsService {

    Optional<CurrencyRateDto> getLatestRate(CurrencyPair pair);
}
