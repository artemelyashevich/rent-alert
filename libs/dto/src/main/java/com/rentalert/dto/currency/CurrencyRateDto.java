package com.rentalert.dto.currency;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Курс {@code 1 currency = rate perCurrency} на дату {@code rateDate}.
 */
public record CurrencyRateDto(
    LocalDate rateDate,
    String currency,
    String perCurrency,
    BigDecimal rate
) {
}
