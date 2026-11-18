package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.roommate_ms.domain.ListingStatusName;
import com.rentalert.roommate_ms.domain.RoommateMatch;
import com.rentalert.roommate_ms.domain.RoommateSharedFavorite;
import com.rentalert.roommate_ms.domain.RoommateSharedFavoriteEmbeddedId;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteCreateDto;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteUpdateDto;
import com.rentalert.roommate_ms.mapper.RoommateSharedFavoriteMapperImpl;
import com.rentalert.roommate_ms.repository.RoommateMatchRepository;
import com.rentalert.roommate_ms.repository.RoommateSharedFavoriteRepository;
import com.rentalert.roommate_ms.service.impl.RoommateSharedFavoriteServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoommateSharedFavoriteServiceImplTests {

    private static final Long MATCH_ID = 7L;
    private static final Long LISTING_ID = 11L;
    private static final Long USER_ID = 5L;

    private final ListingMsService listingMsService = mock(ListingMsService.class);
    private final RoommateMatchRepository matchRepository = mock(RoommateMatchRepository.class);
    private final RoommateSharedFavoriteRepository favoriteRepository = mock(RoommateSharedFavoriteRepository.class);
    private final RoommateListingCards roommateListingCards =
        new RoommateListingCards(listingMsService, new RoommateBatchCache(new InMemoryCache().service()));
    private final RoommateSharedFavoriteService service = new RoommateSharedFavoriteServiceImpl(
        listingMsService, new RoommateMatchAccess(matchRepository), new RoommateSharedFavoriteMapperImpl(), favoriteRepository,
        roommateListingCards);

    @Test
    void repeatedFavoritesListIsServedCardsFromCache() {
        unlockedMatch();
        when(favoriteRepository.findAllByIdMatchId(MATCH_ID)).thenReturn(List.of(favorite(ListingStatusName.WAITING_RESPONSE)));
        when(listingMsService.findCards(any())).thenReturn(List.of(card()));

        service.findAll(MATCH_ID, USER_ID);
        var result = service.findAll(MATCH_ID, USER_ID);

        assertThat(result.get(0).listing().getId()).isEqualTo(LISTING_ID);
        verify(listingMsService, times(1)).findCards(any());
    }

    @Test
    void favoritesAreReturnedWithListingCards() {
        unlockedMatch();
        when(favoriteRepository.findAllByIdMatchId(MATCH_ID)).thenReturn(List.of(favorite(ListingStatusName.WAITING_RESPONSE)));
        when(listingMsService.findCards(any())).thenReturn(List.of(card()));

        var result = service.findAll(MATCH_ID, USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).listing().getId()).isEqualTo(LISTING_ID);
        assertThat(result.get(0).status()).isEqualTo(ListingStatusName.WAITING_RESPONSE);
    }

    @Test
    void favoritesAreReturnedWithoutCardsWhenListingServiceFails() {
        unlockedMatch();
        when(favoriteRepository.findAllByIdMatchId(MATCH_ID)).thenReturn(List.of(favorite(ListingStatusName.INTERESTED)));
        when(listingMsService.findCards(any())).thenThrow(new RuntimeException("listing-ms unavailable"));

        var result = service.findAll(MATCH_ID, USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).listing()).isNull();
        assertThat(result.get(0).status()).isEqualTo(ListingStatusName.INTERESTED);
    }

    @Test
    void lockedMatchForbidsFavorites() {
        lockedMatch();

        assertThatThrownBy(() -> service.findAll(MATCH_ID, USER_ID))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX403_ROOMMATE_MATCH_NOT_UNLOCKED));
        verify(favoriteRepository, never()).findAllByIdMatchId(any());
    }

    @Test
    void addRejectsAlreadySharedListing() {
        unlockedMatch();
        when(favoriteRepository.existsById(embeddedId())).thenReturn(true);

        assertThatThrownBy(() -> service.add(MATCH_ID, USER_ID, createDto()))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX400_ROOMMATE_SHARED_FAVORITE_ALREADY_EXISTS));
        verify(favoriteRepository, never()).saveAndFlush(any());
    }

    @Test
    void addRejectsMissingListing() {
        unlockedMatch();
        when(listingMsService.findCards(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.add(MATCH_ID, USER_ID, createDto()))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX404_LISTING_NOT_FOUND));
        verify(favoriteRepository, never()).saveAndFlush(any());
    }

    @Test
    void addStoresFavoriteWithRequesterAsAuthor() {
        unlockedMatch();
        when(listingMsService.findCards(any())).thenReturn(List.of(card()));
        when(favoriteRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.add(MATCH_ID, USER_ID, createDto());

        verify(favoriteRepository).saveAndFlush(argThat(f ->
            f.getId().equals(embeddedId()) && f.getAddedByUserId().equals(USER_ID)
                && f.getListingStatus() == ListingStatusName.INTERESTED));
        assertThat(result.listing().getId()).isEqualTo(LISTING_ID);
    }

    @Test
    void updateStatusChangesStatusAndViewingDate() {
        unlockedMatch();
        Instant viewing = Instant.parse("2030-01-01T10:00:00Z");
        when(favoriteRepository.findById(embeddedId())).thenReturn(Optional.of(favorite(ListingStatusName.INTERESTED)));
        when(favoriteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(listingMsService.findCards(any())).thenReturn(List.of(card()));

        var result = service.updateStatus(MATCH_ID, USER_ID, LISTING_ID,
            new RoommateSharedFavoriteUpdateDto(ListingStatusName.VIEWING_SCHEDULED, viewing));

        assertThat(result.status()).isEqualTo(ListingStatusName.VIEWING_SCHEDULED);
        assertThat(result.viewingDate()).isEqualTo(viewing);
    }

    @Test
    void removingMissingFavoriteFails() {
        unlockedMatch();
        when(favoriteRepository.findById(embeddedId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(MATCH_ID, USER_ID, LISTING_ID))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX404_ROOMMATE_SHARED_FAVORITE_NOT_FOUND));
        verify(favoriteRepository, never()).delete(any());
    }

    private void unlockedMatch() {
        RoommateMatch match = match();
        match.setContactsUnlockedAt(Instant.now());
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
    }

    private void lockedMatch() {
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match()));
    }

    private static RoommateMatch match() {
        RoommateMatch match = new RoommateMatch();
        match.setId(MATCH_ID);
        match.setUserLoId(USER_ID);
        match.setUserHiId(6L);
        return match;
    }

    private static RoommateSharedFavorite favorite(ListingStatusName status) {
        return RoommateSharedFavorite.builder()
            .id(embeddedId())
            .listingStatus(status)
            .addedByUserId(USER_ID)
            .build();
    }

    private static RoommateSharedFavoriteEmbeddedId embeddedId() {
        return new RoommateSharedFavoriteEmbeddedId(MATCH_ID, LISTING_ID);
    }

    private static RoommateSharedFavoriteCreateDto createDto() {
        return new RoommateSharedFavoriteCreateDto(LISTING_ID, ListingStatusName.INTERESTED, null);
    }

    private static ListingDto card() {
        return ListingDto.builder().id(LISTING_ID).title("Flat").build();
    }
}
