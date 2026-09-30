package com.rentalert.internal.client;

import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = ProcessingMsClient.SERVICE_NAME, path = "/processing/internal")
public interface ProcessingMsClient {

    String SERVICE_NAME = "processing-ms";

    @GetMapping("/currency-rates/{pair}")
    CurrencyRateDto getLatestRate(@PathVariable("pair") CurrencyPair pair);
}
