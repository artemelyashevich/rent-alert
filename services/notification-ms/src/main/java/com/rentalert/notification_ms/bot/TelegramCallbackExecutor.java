package com.rentalert.notification_ms.bot;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface TelegramCallbackExecutor {

    String getPrefix();

    TelegramCallbackResult execute(CallbackQuery callbackQuery, String payload);
}
