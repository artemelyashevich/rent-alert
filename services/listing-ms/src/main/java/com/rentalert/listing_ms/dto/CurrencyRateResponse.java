package com.rentalert.listing_ms.dto;

import com.rentalert.listing_ms.domain.CurrencyRate;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с курсами валют")
public class CurrencyRateResponse {

    @Schema(description = "Дата актуальности курсов", example = "2026-08-11")
    private LocalDate rateDate;

    @Schema(description = "Список курсов валют")
    private List<CurrencyRateDto> rates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Курс валюты")
    public static class CurrencyRateDto {

        @Schema(description = "Базовая валюта", example = "BYN")
        private String currency;

        @Schema(description = "Валюта, к которой применяется курс", example = "USD")
        private String perCurrency;

        @Schema(description = "Курс обмена", example = "3.25")
        private BigDecimal rate;
    }

    public static CurrencyRateResponse fromRates(List<CurrencyRate> currencyRates) {
        if (currencyRates == null || currencyRates.isEmpty()) {
            return CurrencyRateResponse.builder()
                    .rateDate(LocalDate.now())
                    .rates(List.of())
                    .build();
        }

        LocalDate latestDate = currencyRates.stream()
                .map(CurrencyRate::getRateDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        List<CurrencyRateDto> rateDtos = currencyRates.stream()
                .map(rate -> CurrencyRateDto.builder()
                        .currency(rate.getCurrency())
                        .perCurrency(rate.getPerCurrency())
                        .rate(rate.getRate().setScale(2, RoundingMode.HALF_UP))
                        .build())
                .toList();

        return CurrencyRateResponse.builder()
                .rateDate(latestDate)
                .rates(rateDtos)
                .build();
    }
}
