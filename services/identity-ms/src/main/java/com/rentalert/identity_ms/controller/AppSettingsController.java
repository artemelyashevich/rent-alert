package com.rentalert.identity_ms.controller;

import com.rentalert.dto.AppSettingsResponse;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.identity_ms.config.PublicUrls;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "App settings", description = "Справочники для фронтенда")
@RestController
@RequestMapping(PublicUrls.APP_SETTINGS_TAG)
public class AppSettingsController {

    @Operation(summary = "Провайдеры, статусы бота, валюты и доступные каналы уведомлений")
    @GetMapping
    public AppSettingsResponse findAll() {
        return new AppSettingsResponse(
            List.of(ScheduleProvider.values()),
            List.of(BotStatus.values()),
            List.of(Currency.values()),
            List.of(CurrencyPair.values()),
            Stream.of(NotificationEngine.values())
                .filter(engine -> !engine.isForAdmin() && engine.isEnabled() && engine != NotificationEngine.WEBSOCKET)
                .toList()
        );
    }
}
