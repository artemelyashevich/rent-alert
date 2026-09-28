package com.rentalert.notification_ms.bot;

import lombok.NonNull;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface TelegramMessageService {

    SendMessage processMessage(TelegramCreateMessage telegramMessage);

    void handleBotBlocked(@NonNull String chatId);
}
