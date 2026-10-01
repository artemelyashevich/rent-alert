package com.rentalert.notification_ms.bot;

import com.rentalert.notification_ms.domain.TelegramMessage;
import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
public class TelegramStartCommandExecutor implements TelegramCommandExecutor {

    private static final String START_TEXT = """
            👋 Привет! Это RentAlert — бот, который следит за объявлениями об аренде жилья на Kufar, Realt.by и Onliner и присылает уведомление, как только появляется что-то под ваш фильтр.

            Что умеет бот:
            • 🔔 моментальные алерты о новых объявлениях по вашим параметрам
            • ⭐ избранное и 🙈 скрытие объявлений прямо в чате
            • ⏰ напоминания с экспортом в календарь
            • 🌙 тихие часы — ночью уведомления придут позже
            • 💱 актуальные курсы валют

            С чего начать:
            /app — открыть приложение и настроить фильтры
            /help — список всех команд

            Выберите действие на клавиатуре ниже 👇
            """;

    @Override
    public TelegramCommand getCommand() {
        return TelegramCommand.START;
    }

    @Override
    public SendMessage execute(TelegramMessage telegramMessage, String chatId) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("/app"));
        row1.add(new KeyboardButton("/start"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("/help"));

        ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
            .keyboard(List.of(row1, row2))
            .resizeKeyboard(true)
            .oneTimeKeyboard(false)
            .build();

        return SendMessage.builder()
            .chatId(chatId)
            .text(START_TEXT)
            .replyMarkup(keyboard)
            .build();
    }
}
