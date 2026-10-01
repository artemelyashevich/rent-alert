package com.rentalert.processing_ms.repository.projection;

import com.rentalert.dto.currency.Currency;
import java.math.BigDecimal;

public interface CurrencyPriceStatProjection {

    Currency getCurrency();

    long getTotal();

    Double getAvgPrice();

    BigDecimal getMinPrice();

    BigDecimal getMaxPrice();
}
