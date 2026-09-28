package com.rentalert.notification_ms.bot;

import com.rentalert.common.exception.AppException;
import java.util.Arrays;
import org.springframework.http.HttpStatus;

public enum TelegramCommand {
    START,
    APP,
    HELP;

    public static TelegramCommand getCommand(String name) {
        if (name == null || name.isBlank()) {
            throw new AppException("Invalid tg command", HttpStatus.BAD_REQUEST);
        }

        String normalized = name.startsWith("/")
            ? name.substring(1)
            : name;

        return Arrays.stream(values())
            .filter(command -> command.name().equalsIgnoreCase(normalized))
            .findFirst()
            .orElseThrow(() -> new AppException(
                "Invalid tg command",
                HttpStatus.BAD_REQUEST
            ));
    }
}
