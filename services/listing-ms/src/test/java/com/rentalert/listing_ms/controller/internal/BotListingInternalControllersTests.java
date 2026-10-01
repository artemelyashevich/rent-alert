package com.rentalert.listing_ms.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.listing.FavoriteAddResult;
import com.rentalert.listing_ms.domain.ListingStatus;
import com.rentalert.listing_ms.domain.ListingStatusName;
import com.rentalert.listing_ms.dto.FavoriteCreateDto;
import com.rentalert.listing_ms.service.FavoriteService;
import com.rentalert.listing_ms.service.HiddenListingService;
import com.rentalert.listing_ms.service.ListingStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BotListingInternalControllersTests {

    private final FavoriteService favoriteService = mock(FavoriteService.class);
    private final ListingStatusService listingStatusService = mock(ListingStatusService.class);
    private final HiddenListingService hiddenListingService = mock(HiddenListingService.class);
    private final FavoriteInternalController favorites = new FavoriteInternalController(favoriteService, listingStatusService);
    private final HiddenListingInternalController hidden = new HiddenListingInternalController(hiddenListingService);

    private static AppException error(InternalExceptionCode code) {
        return new AppException(code, HttpStatus.BAD_REQUEST);
    }

    @BeforeEach
    void interestedStatus() {
        ListingStatus status = ListingStatus.builder().name(ListingStatusName.INTERESTED).build();
        status.setId(3L);
        when(listingStatusService.findByName(ListingStatusName.INTERESTED)).thenReturn(status);
    }

    @Test
    void addsFavoriteWithInterestedStatus() {
        assertThat(favorites.add(5L, 7L)).isEqualTo(FavoriteAddResult.ADDED);
        verify(favoriteService).addToFavorites(5L, new FavoriteCreateDto(7L, 3L, null));
    }

    @Test
    void reportsExistingFavoriteAndLimit() {
        when(favoriteService.addToFavorites(eq(5L), any()))
            .thenThrow(error(InternalExceptionCode.EX400_FAVORITE_ALREADY_EXISTS))
            .thenThrow(error(InternalExceptionCode.EX400_FAVORITE_LIMIT_REACHED))
            .thenThrow(error(InternalExceptionCode.EX404_LISTING_NOT_FOUND));

        assertThat(favorites.add(5L, 7L)).isEqualTo(FavoriteAddResult.ALREADY_EXISTS);
        assertThat(favorites.add(5L, 7L)).isEqualTo(FavoriteAddResult.LIMIT_REACHED);
        assertThatThrownBy(() -> favorites.add(5L, 7L)).isInstanceOf(AppException.class);
    }

    @Test
    void removeReportsMissingFavorite() {
        doThrow(error(InternalExceptionCode.EX404_FAVORITE_NOT_FOUND)).when(favoriteService).removeFromFavorites(5L, 8L);

        assertThat(favorites.remove(5L, 7L)).isTrue();
        assertThat(favorites.remove(5L, 8L)).isFalse();
    }

    @Test
    void hideReportsAlreadyHidden() {
        doThrow(error(InternalExceptionCode.EX400_HIDDEN_LISTING_ALREADY_EXISTS)).when(hiddenListingService).hideListing(5L, 8L);
        doThrow(error(InternalExceptionCode.EX404_LISTING_NOT_FOUND)).when(hiddenListingService).hideListing(5L, 9L);

        assertThat(hidden.hide(5L, 7L)).isTrue();
        assertThat(hidden.hide(5L, 8L)).isFalse();
        assertThatThrownBy(() -> hidden.hide(5L, 9L)).isInstanceOf(AppException.class);
    }
}
