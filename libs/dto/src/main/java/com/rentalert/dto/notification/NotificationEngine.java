package com.rentalert.dto.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationEngine {
    TELEGRAM_ADMIN(true, true),
    EMAIL(false, true),
    PUSH(false, false),
    TELEGRAM_BOT(false, true),
    WEBSOCKET(false, true),
    SMS(false, false);

    private final boolean isForAdmin;
    private final boolean isEnabled;
}
