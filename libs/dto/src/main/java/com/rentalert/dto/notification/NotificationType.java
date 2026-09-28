package com.rentalert.dto.notification;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    APP_STARTED(NotificationCategory.OPS_ALERTS, List.of(NotificationEngine.TELEGRAM_ADMIN)),
    APP_STOPPED(NotificationCategory.OPS_ALERTS, List.of(NotificationEngine.TELEGRAM_ADMIN)),
    NEW_LISTING_ADMIN_ALERT(NotificationCategory.SCRAPER_ALERTS, List.of(NotificationEngine.TELEGRAM_ADMIN)),
    USER_CREATED(NotificationCategory.OPS_ALERTS, List.of(NotificationEngine.TELEGRAM_ADMIN)),

    NEW_LISTING_MATCHED(NotificationCategory.LISTING_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    FAVORITE_REMINDER(NotificationCategory.REMINDER_ALERTS , List.of(
            NotificationEngine.TELEGRAM_BOT
    )),
    EMAIL_ACTIVATION(NotificationCategory.EMAIL_ALERTS, List.of(NotificationEngine.EMAIL)),
    EMAIL_ACTIVATION_SUCCESS(NotificationCategory.EMAIL_ALERTS, List.of(NotificationEngine.EMAIL)),
    NEW_FEEDBACK(NotificationCategory.OPS_ALERTS, List.of(NotificationEngine.TELEGRAM_ADMIN)),
    BOT_BLOCKED(NotificationCategory.OPS_ALERTS, List.of(NotificationEngine.TELEGRAM_ADMIN)),
    BOT_UNBLOCKED(NotificationCategory.OPS_ALERTS, List.of(NotificationEngine.TELEGRAM_ADMIN)),
    ADMIN_BROADCAST(NotificationCategory.BROADCAST_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),

    // Соседи (co-living matching) — шаг 5
    ROOMMATE_NEW_INTEREST(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    ROOMMATE_NEW_CANDIDATES(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    ROOMMATE_MATCHED(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    ROOMMATE_CHAT_STARTED(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    ROOMMATE_VIEWING_SCHEDULED(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    ROOMMATE_VIEWING_REMINDER(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    ROOMMATE_MATCH_ENDED(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    ROOMMATE_INTEREST_EXPIRED(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    ROOMMATE_PROFILE_APPROVED(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT)),
    ROOMMATE_PROFILE_REJECTED(NotificationCategory.ROOMMATE_ALERTS, List.of(NotificationEngine.TELEGRAM_BOT));

    private final NotificationCategory category;
    private final List<NotificationEngine> defaultEngines;
}