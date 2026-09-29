package com.rentalert.processing_ms.dto;

import java.math.BigDecimal;

public record FrankfurterRateResponse(
    String date,
    String base,
    String quote,
    BigDecimal rate
) {
}
