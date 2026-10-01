package com.rentalert.notification_ms.bot;

import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnfavoriteListingCallbackExecutor implements TelegramCallbackExecutor {

    private static final String FAILED_TEXT = "Не удалось удалить из избранных";

    private final ListingMsService listingMsService;
    private final ListingActionsKeyboardFactory keyboardFactory;
    private final UserEventPublisher userEventPublisher;

    @Override
    public String getPrefix() {
        return ListingActionsKeyboardFactory.UNFAVORITE_CALLBACK_PREFIX;
    }

    @Override
    public TelegramCallbackResult execute(Long userId, String payload) {
        Long listingId;
        try {
            listingId = Long.parseLong(payload);
        } catch (NumberFormatException e) {
            log.warn("Invalid listingId in unfavorite callback payload: {}", payload);
            return TelegramCallbackResult.of(FAILED_TEXT, false);
        }

        boolean removed;
        try {
            removed = listingMsService.removeFavorite(userId, listingId);
        } catch (Exception e) {
            log.warn("Failed to remove listing from favorites via bot: userId={}, listingId={}", userId, listingId, e);
            return TelegramCallbackResult.of(FAILED_TEXT, false);
        }
        if (!removed) {
            return TelegramCallbackResult.of("Уже не в избранном", false, keyboardFactory.build(listingId, false));
        }
        userEventPublisher.publishBotAction(UserEvent.CALLBACK_UNFAVORITE, userId, listingId);
        return TelegramCallbackResult.of("Удалено из избранных", false, keyboardFactory.build(listingId, false));
    }
}
