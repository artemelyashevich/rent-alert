package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.listing_ms.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrencyRateEventListener {

    private final CurrencyRateService currencyRateService;

    @KafkaListener(topics = KafkaTopics.CURRENCY_RATES, groupId = "listing.currency-rates")
    public void onCurrencyRate(CurrencyRateDto rate) {
        if (rate.rateDate() == null || rate.currency() == null || rate.perCurrency() == null || rate.rate() == null) {
            throw new NonRetryableEventException("Currency rate event with missing fields");
        }
        currencyRateService.saveRate(rate);
    }
}
