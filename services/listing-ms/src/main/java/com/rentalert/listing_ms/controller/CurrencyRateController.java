package com.rentalert.listing_ms.controller;

import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.listing_ms.dto.CurrencyRateResponse;
import com.rentalert.listing_ms.service.CurrencyRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Currency Rates", description = "Курсы валют Национального банка РБ")
@RestController
@RequestMapping(PublicUrls.CURRENCY_RATE_TAG)
@RequiredArgsConstructor
public class CurrencyRateController {

    private final CurrencyRateService currencyRateService;

    @Operation(
            summary = "Получить актуальные курсы валют",
            description = "Возвращает последние доступные курсы валют из базы данных"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Курсы валют успешно получены",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CurrencyRateResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping
    public CurrencyRateResponse getCurrencyRates() {
        return currencyRateService.getAllLatestRates();
    }
}
