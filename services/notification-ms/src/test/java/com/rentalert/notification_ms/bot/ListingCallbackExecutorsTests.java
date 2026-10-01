package com.rentalert.notification_ms.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.listing.FavoriteAddResult;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.notification_ms.messaging.UserEventPublisher;
import org.junit.jupiter.api.Test;

class ListingCallbackExecutorsTests {

    private final ListingMsService listingMsService = mock(ListingMsService.class);
    private final UserEventPublisher userEventPublisher = mock(UserEventPublisher.class);
    private final ListingActionsKeyboardFactory keyboardFactory = new ListingActionsKeyboardFactory();
    private final FavoriteListingCallbackExecutor favorite = new FavoriteListingCallbackExecutor(listingMsService, keyboardFactory, userEventPublisher);
    private final UnfavoriteListingCallbackExecutor unfavorite = new UnfavoriteListingCallbackExecutor(listingMsService, keyboardFactory, userEventPublisher);
    private final HideListingCallbackExecutor hide = new HideListingCallbackExecutor(listingMsService, userEventPublisher);

    private static String firstButtonData(TelegramCallbackResult result) {
        return result.updatedMarkup().getKeyboard().getFirst().getFirst().getCallbackData();
    }

    @Test
    void favoriteSwitchesButtonToUnfavorite() {
        when(listingMsService.addFavorite(5L, 7L)).thenReturn(FavoriteAddResult.ADDED);

        TelegramCallbackResult result = favorite.execute(5L, "7");

        assertThat(result.answerText()).isEqualTo("Добавлено в избранное ⭐");
        assertThat(firstButtonData(result)).isEqualTo("unfavorite:7");
        verify(userEventPublisher).publishBotAction(UserEvent.CALLBACK_FAVORITE, 5L, 7L);
    }

    @Test
    void favoriteAlreadyAddedIsNotCountedAgain() {
        when(listingMsService.addFavorite(5L, 7L)).thenReturn(FavoriteAddResult.ALREADY_EXISTS);

        TelegramCallbackResult result = favorite.execute(5L, "7");

        assertThat(result.answerText()).isEqualTo("Уже в избранном");
        assertThat(firstButtonData(result)).isEqualTo("unfavorite:7");
        verifyNoInteractions(userEventPublisher);
    }

    @Test
    void favoriteLimitKeepsButtons() {
        when(listingMsService.addFavorite(5L, 7L)).thenReturn(FavoriteAddResult.LIMIT_REACHED);

        TelegramCallbackResult result = favorite.execute(5L, "7");

        assertThat(result.answerText()).isEqualTo("Достигнут лимит избранного (30)");
        assertThat(result.updatedMarkup()).isNull();
    }

    @Test
    void favoriteFailureIsReportedToUser() {
        when(listingMsService.addFavorite(5L, 7L)).thenThrow(new UpstreamHttpException("down", 503, null));

        assertThat(favorite.execute(5L, "7").answerText()).isEqualTo("Не удалось добавить в избранное");
        assertThat(favorite.execute(5L, "abc").answerText()).isEqualTo("Не удалось добавить в избранное");
        verifyNoInteractions(userEventPublisher);
    }

    @Test
    void unfavoriteSwitchesButtonToFavorite() {
        when(listingMsService.removeFavorite(5L, 7L)).thenReturn(true);

        TelegramCallbackResult result = unfavorite.execute(5L, "7");

        assertThat(result.answerText()).isEqualTo("Удалено из избранных");
        assertThat(firstButtonData(result)).isEqualTo("favorite:7");
        verify(userEventPublisher).publishBotAction(UserEvent.CALLBACK_UNFAVORITE, 5L, 7L);
    }

    @Test
    void unfavoriteOfMissingFavorite() {
        when(listingMsService.removeFavorite(5L, 7L)).thenReturn(false);

        TelegramCallbackResult result = unfavorite.execute(5L, "7");

        assertThat(result.answerText()).isEqualTo("Уже не в избранном");
        assertThat(firstButtonData(result)).isEqualTo("favorite:7");
        verifyNoInteractions(userEventPublisher);
    }

    @Test
    void hideDeletesMessage() {
        when(listingMsService.hideListing(5L, 7L)).thenReturn(true);

        TelegramCallbackResult result = hide.execute(5L, "7");

        assertThat(result.deleteMessage()).isTrue();
        verify(userEventPublisher).publishBotAction(UserEvent.CALLBACK_HIDE, 5L, 7L);
    }

    @Test
    void hideDeletesMessageEvenOnFailure() {
        when(listingMsService.hideListing(any(), any())).thenThrow(new UpstreamHttpException("not found", 404, null));

        assertThat(hide.execute(5L, "7").deleteMessage()).isTrue();
        verifyNoInteractions(userEventPublisher);
    }
}
