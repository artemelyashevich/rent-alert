package com.rentalert.listing_ms.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.listing_ms.dto.FavoriteCreateDto;
import com.rentalert.listing_ms.mapper.FavoriteMapper;
import com.rentalert.listing_ms.messaging.OpsEventPublisher;
import com.rentalert.listing_ms.repository.FavoriteRepository;
import com.rentalert.listing_ms.repository.ListingRepository;
import com.rentalert.listing_ms.service.impl.FavoriteServiceImpl;
import org.junit.jupiter.api.Test;

class FavoriteServiceImplTests {

    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final ListingRepository listingRepository = mock(ListingRepository.class);
    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final FavoriteService service = new FavoriteServiceImpl(
        favoriteRepository,
        listingRepository,
        mock(ListingStatusService.class),
        mock(FavoriteMapper.class),
        mock(OpsEventPublisher.class),
        identityMsService
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
}
