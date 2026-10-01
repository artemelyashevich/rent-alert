package com.rentalert.listing_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.listing.FavoriteViewingDto;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.listing_ms.domain.Favorite;
import com.rentalert.listing_ms.domain.FavoriteEmbeddedId;
import com.rentalert.listing_ms.domain.Listing;
import com.rentalert.listing_ms.dto.FavoriteCreateDto;
import com.rentalert.listing_ms.mapper.FavoriteMapper;
import com.rentalert.listing_ms.mapper.ListingMapper;
import com.rentalert.listing_ms.messaging.OpsEventPublisher;
import com.rentalert.listing_ms.repository.FavoriteRepository;
import com.rentalert.listing_ms.repository.ListingRepository;
import com.rentalert.listing_ms.service.impl.FavoriteServiceImpl;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FavoriteServiceImplTests {

    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final ListingRepository listingRepository = mock(ListingRepository.class);
    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final ListingMapper listingMapper = mock(ListingMapper.class);
    private final FavoriteService service = new FavoriteServiceImpl(
        favoriteRepository,
        listingRepository,
        mock(ListingStatusService.class),
        mock(FavoriteMapper.class),
        mock(OpsEventPublisher.class),
        identityMsService,
        listingMapper
    );

    private final FavoriteCreateDto createDto = new FavoriteCreateDto(15L, 1L, null);

    @Test
    void unknownUserIsNotFound() {
        when(identityMsService.getProfile(42L)).thenThrow(new UpstreamHttpException("not found", 404, null));

        assertThatThrownBy(() -> service.addToFavorites(42L, createDto))
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(InternalExceptionCode.EX404_USER_NOT_FOUND);
        verify(listingRepository, never()).findById(any());
    }

    @Test
    void identityFailureIsPropagated() {
        UpstreamHttpException unavailable = new UpstreamHttpException("unavailable", 503, null);
        when(identityMsService.getProfile(42L)).thenThrow(unavailable);

        assertThatThrownBy(() -> service.addToFavorites(42L, createDto)).isSameAs(unavailable);
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void viewingCarriesDateAndListing() {
        Instant viewingDate = Instant.parse("2026-10-05T12:00:00Z");
        Listing listing = new Listing();
        ListingDto listingDto = ListingDto.builder().id(15L).title("2-комн.").build();
        FavoriteEmbeddedId id = new FavoriteEmbeddedId(42L, 15L);
        when(favoriteRepository.findById(id))
            .thenReturn(Optional.of(Favorite.builder().id(id).listing(listing).viewingDate(viewingDate).build()));
        when(listingMapper.toDto(listing)).thenReturn(listingDto);

        assertThat(service.findViewing(42L, 15L))
            .contains(new FavoriteViewingDto(42L, 15L, viewingDate, listingDto));
    }

    @Test
    void viewingOfMissingFavoriteIsEmpty() {
        when(favoriteRepository.findById(new FavoriteEmbeddedId(42L, 15L))).thenReturn(Optional.empty());

        assertThat(service.findViewing(42L, 15L)).isEmpty();
    }
}
