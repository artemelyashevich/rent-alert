package com.rentalert.dto.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What happened, as producers see it. How it is delivered (category, channels, mandatory, on/off)
 * is notification's own data: the notification_type_settings table.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {

    APP_STARTED(false),
    APP_STOPPED(false),
    NEW_LISTING_ADMIN_ALERT(false),
    USER_CREATED(false),

    NEW_LISTING_MATCHED(false),
    FAVORITE_REMINDER(false),
    EMAIL_ACTIVATION(true),
    EMAIL_ACTIVATION_SUCCESS(true),
    NEW_FEEDBACK(false),
    BOT_BLOCKED(false),
    BOT_UNBLOCKED(false),
    ADMIN_BROADCAST(false),

    // Соседи (co-living matching) — шаг 5
    ROOMMATE_NEW_INTEREST(false),
    ROOMMATE_NEW_CANDIDATES(false),
    ROOMMATE_MATCHED(false),
    ROOMMATE_CHAT_STARTED(false),
    ROOMMATE_VIEWING_SCHEDULED(false),
    ROOMMATE_VIEWING_REMINDER(false),
    ROOMMATE_MATCH_ENDED(false),
    ROOMMATE_INTEREST_EXPIRED(false),
    ROOMMATE_PROFILE_APPROVED(false),
    ROOMMATE_PROFILE_REJECTED(false);

    /**
     * The user is waiting for it right now (email codes): goes to the priority topic, so a mass mailing
     * never delays it. Producers route by this, so it lives in code, not in notification's database.
     */
    private final boolean priority;
}
