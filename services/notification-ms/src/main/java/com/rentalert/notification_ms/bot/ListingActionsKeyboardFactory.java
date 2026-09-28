package com.rentalert.notification_ms.bot;

import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Component
public class ListingActionsKeyboardFactory {

    public static final String FAVORITE_CALLBACK_PREFIX = "favorite";
    public static final String UNFAVORITE_CALLBACK_PREFIX = "unfavorite";
    public static final String HIDE_CALLBACK_PREFIX = "hide";

    public InlineKeyboardMarkup build(Long listingId, boolean isFavorite) {
        InlineKeyboardButton favoriteButton = isFavorite
            ? InlineKeyboardButton.builder()
                .text("💔 Удалить из избранных")
                .callbackData(UNFAVORITE_CALLBACK_PREFIX + ":" + listingId)
                .build()
            : InlineKeyboardButton.builder()
                .text("⭐ Добавить в избранное")
                .callbackData(FAVORITE_CALLBACK_PREFIX + ":" + listingId)
                .build();
        InlineKeyboardButton hideButton = InlineKeyboardButton.builder()
            .text("🗑 Скрыть")
            .callbackData(HIDE_CALLBACK_PREFIX + ":" + listingId)
            .build();
        return InlineKeyboardMarkup.builder()
            .keyboard(List.of(List.of(favoriteButton), List.of(hideButton)))
            .build();
    }
}
