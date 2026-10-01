package com.rentalert.notification_ms.bot;

import com.rentalert.notification_ms.domain.TelegramMessage;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class TelegramHelpCommandExecutor implements TelegramCommandExecutor {

    private static final String HELP_TEXT = """
            🤖 Список доступных команд:

            /start — перезапустить бота и открыть главное меню
            /app — открыть приложение
            /help — показать это сообщение

            Под каждым новым объявлением есть кнопки «В избранное» и «Скрыть» — ими можно управлять списком объявлений прямо в чате.
            """;

    @Override
    public TelegramCommand getCommand() {
        return TelegramCommand.HELP;
    }

    @Override
    public SendMessage execute(TelegramMessage telegramMessage, String chatId) {
        return SendMessage.builder()
            .chatId(chatId)
            .text(HELP_TEXT)
            .build();
    }
}
