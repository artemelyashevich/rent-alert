package com.rentalert.dto;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.profile.BotStatus;
import java.util.List;

public record AppSettingsResponse(
    List<ScheduleProvider> providers,
    List<BotStatus> botStatuses,
    List<Currency> currencies,
    List<CurrencyPair> currencyPairs,
    List<NotificationEngine> notificationEngines
) {

}
