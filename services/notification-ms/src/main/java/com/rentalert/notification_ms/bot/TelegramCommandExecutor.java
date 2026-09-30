package com.rentalert.notification_ms.bot;

import com.rentalert.notification_ms.domain.TelegramMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface TelegramCommandExecutor {

    TelegramCommand getCommand();

    SendMessage execute(TelegramMessage telegramMessage);
}
