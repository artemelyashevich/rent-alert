package com.rentalert.notification_ms.bot;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface TelegramCallbackService {

    TelegramCallbackResult processCallback(CallbackQuery callbackQuery);
}
