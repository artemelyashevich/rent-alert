package com.rentalert.notification_ms.bot;

import com.rentalert.notification_ms.domain.TelegramMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class TelegramAppCommandExecutor implements TelegramCommandExecutor {

    private final OpenAppKeyboardFactory openAppKeyboardFactory;

    @Override
    public TelegramCommand getCommand() {
        return TelegramCommand.APP;
    }

    @Override
    public SendMessage execute(TelegramMessage telegramMessage, String chatId) {
        return SendMessage.builder()
            .chatId(chatId)
            .text("App")
            .replyMarkup(openAppKeyboardFactory.build())
            .build();
    }
}
