package com.rentalert.notification_ms.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public record TelegramCallbackResult(String answerText, boolean deleteMessage, InlineKeyboardMarkup updatedMarkup) {

    public static TelegramCallbackResult of(String answerText, boolean deleteMessage) {
        return new TelegramCallbackResult(answerText, deleteMessage, null);
    }

    public static TelegramCallbackResult of(String answerText, boolean deleteMessage, InlineKeyboardMarkup updatedMarkup) {
        return new TelegramCallbackResult(answerText, deleteMessage, updatedMarkup);
    }
}
