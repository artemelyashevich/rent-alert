package com.rentalert.notification_ms.bot;

import com.rentalert.notification_ms.config.ApplicationProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

@Component
@RequiredArgsConstructor
public class OpenAppKeyboardFactory {

    private final ApplicationProperties applicationProperties;

    public InlineKeyboardMarkup build() {
        InlineKeyboardButton openApp = InlineKeyboardButton.builder()
            .text("🚀 Открыть приложение")
            .webApp(new WebAppInfo(applicationProperties.getTelegram().getWebAppUrl()))
            .build();
        return InlineKeyboardMarkup.builder()
            .keyboard(List.of(List.of(openApp)))
            .build();
    }
}
