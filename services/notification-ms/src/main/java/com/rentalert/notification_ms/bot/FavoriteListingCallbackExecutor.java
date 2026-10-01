package com.rentalert.notification_ms.bot;

import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.listing.FavoriteAddResult;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FavoriteListingCallbackExecutor implements TelegramCallbackExecutor {

    private static final String FAILED_TEXT = "Не удалось добавить в избранное";

    private final ListingMsService listingMsService;
    private final ListingActionsKeyboardFactory keyboardFactory;
    private final UserEventPublisher userEventPublisher;

    @Override
    public String getPrefix() {
        return ListingActionsKeyboardFactory.FAVORITE_CALLBACK_PREFIX;
    }

    @Override
    public TelegramCallbackResult execute(Long userId, String payload) {
        Long listingId;
        try {
            listingId = Long.parseLong(payload);
        } catch (NumberFormatException e) {
            log.warn("Invalid listingId in favorite callback payload: {}", payload);
            return TelegramCallbackResult.of(FAILED_TEXT, false);
        }

        FavoriteAddResult result;
        try {
            result = listingMsService.addFavorite(userId, listingId);
        } catch (Exception e) {
            log.warn("Failed to add listing to favorites via bot: userId={}, listingId={}", userId, listingId, e);
            return TelegramCallbackResult.of(FAILED_TEXT, false);
        }
        return switch (result) {
            case ADDED -> {
                userEventPublisher.publishBotAction(UserEvent.CALLBACK_FAVORITE, userId, listingId);
                yield TelegramCallbackResult.of("Добавлено в избранное ⭐", false, keyboardFactory.build(listingId, true));
            }
            case ALREADY_EXISTS -> TelegramCallbackResult.of("Уже в избранном", false, keyboardFactory.build(listingId, true));
            case LIMIT_REACHED -> TelegramCallbackResult.of("Достигнут лимит избранного (30)", false);
        };
    }
}
