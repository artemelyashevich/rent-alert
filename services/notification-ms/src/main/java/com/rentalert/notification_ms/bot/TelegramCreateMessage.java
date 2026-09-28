package com.rentalert.notification_ms.bot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramCreateMessage {
    private String chatId;

    private String telegramId;

    private String username;

    private String text;

    private TelegramCommand command;
}
