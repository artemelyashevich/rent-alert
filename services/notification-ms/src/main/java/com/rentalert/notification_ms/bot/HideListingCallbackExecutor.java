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
public class HideListingCallbackExecutor implements TelegramCallbackExecutor {

    private static final String HIDDEN_ANSWER_TEXT = "Объявление скрыто";

    private final ListingMsService listingMsService;
    private final UserEventPublisher userEventPublisher;

    @Override
    public String getPrefix() {
        return ListingActionsKeyboardFactory.HIDE_CALLBACK_PREFIX;
    }

    @Override
    public TelegramCallbackResult execute(Long userId, String payload) {
        Long listingId;
        try {
            listingId = Long.parseLong(payload);
        } catch (NumberFormatException e) {
            log.warn("Invalid listingId in hide callback payload: {}", payload);
            return TelegramCallbackResult.of(HIDDEN_ANSWER_TEXT, true);
        }

        try {
            if (listingMsService.hideListing(userId, listingId)) {
                userEventPublisher.publishBotAction(UserEvent.CALLBACK_HIDE, userId, listingId);
            }
        } catch (Exception e) {
            log.warn("Failed to hide listing via bot: userId={}, listingId={}", userId, listingId, e);
        }
        return TelegramCallbackResult.of(HIDDEN_ANSWER_TEXT, true);
    }
}
