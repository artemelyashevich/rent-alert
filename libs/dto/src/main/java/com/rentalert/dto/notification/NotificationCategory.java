package com.rentalert.dto.notification;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationCategory {

    OPS_ALERTS(
        false,
        List.of(NotificationEngine.TELEGRAM_ADMIN),
        "Операционные уведомления"
    ),
    EMAIL_ALERTS(false, List.of(NotificationEngine.EMAIL), "Уведомления по почте"),
    LISTING_ALERTS(
        false,
        List.of(NotificationEngine.TELEGRAM_ADMIN, NotificationEngine.TELEGRAM_BOT),
        "Новые объявления"
    ),
    REMINDER_ALERTS(false, List.of(), "Напоминание о новом просмотре"),
    SCRAPER_ALERTS(false, List.of(NotificationEngine.TELEGRAM_ADMIN), "Новые объявления для администратора"),
    BROADCAST_ALERTS(false, List.of(NotificationEngine.TELEGRAM_BOT), "Рассылка от администратора"),
    ROOMMATE_ALERTS(false, List.of(), "Уведомления раздела «Соседи»");

    private final boolean mandatory;
    private final List<NotificationEngine> mandatoryEngines;
    private final String displayName;
}