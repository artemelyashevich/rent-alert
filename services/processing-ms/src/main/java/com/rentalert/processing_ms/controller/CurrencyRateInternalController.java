package com.rentalert.processing_ms.controller;

import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.processing_ms.config.PublicUrls;
import com.rentalert.processing_ms.domain.CurrencyRate;
import com.rentalert.processing_ms.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_CURRENCY_RATE_TAG)
@RequiredArgsConstructor
public class CurrencyRateInternalController {

    private final CurrencyRateService currencyRateService;

    @GetMapping("/{pair}")
    public CurrencyRateDto getLatestRate(@PathVariable CurrencyPair pair) {
        CurrencyRate rate = currencyRateService.getLatestRate(pair);
        return rate == null
            ? null
            : new CurrencyRateDto(rate.getRateDate(), rate.getCurrency(), rate.getPerCurrency(), rate.getRate());
    }
}
