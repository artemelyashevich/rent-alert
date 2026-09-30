package com.rentalert.listing_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.internal.client.service.ProcessingMsService;
import com.rentalert.listing_ms.service.impl.CurrencyRateServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CurrencyRateServiceImplTests {

    private final ProcessingMsService processingMsService = mock(ProcessingMsService.class);
    private final CurrencyRateService service = new CurrencyRateServiceImpl(processingMsService);

    @Test
    void convertsWithRateFromProcessing() {
        when(processingMsService.getLatestRate(CurrencyPair.USD_BYN)).thenReturn(Optional.of(new CurrencyRateDto(LocalDate.of(2026, 9, 30), "USD", "BYN", new BigDecimal("3.25"))));

        assertThat(service.convert(new BigDecimal("300"), Currency.USD, Currency.BYN)).contains(new BigDecimal("975.00"));
    }

    @Test
    void noConversionWithoutRate() {
        when(processingMsService.getLatestRate(CurrencyPair.USD_BYN)).thenReturn(Optional.empty());

        assertThat(service.getLatestRate(CurrencyPair.USD_BYN)).isNull();
        assertThat(service.convert(new BigDecimal("300"), Currency.USD, Currency.BYN)).isEmpty();
    }
}
