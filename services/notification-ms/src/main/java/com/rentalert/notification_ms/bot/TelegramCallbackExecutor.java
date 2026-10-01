package com.rentalert.notification_ms.bot;

public interface TelegramCallbackExecutor {

    String getPrefix();

    TelegramCallbackResult execute(Long userId, String payload);
}
