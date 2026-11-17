package com.rentalert.listing_ms.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.listing_ms.messaging.ListingEventPublisher;
import com.rentalert.listing_ms.repository.FavoriteRepository;
import com.rentalert.listing_ms.repository.HiddenListingRepository;
import com.rentalert.listing_ms.repository.ListingRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class ListingCleanupParticipantTests {

    private static final Instant MIDNIGHT = Instant.parse("2026-09-30T21:00:00Z");

    private final ListingRepository listingRepository = mock(ListingRepository.class);
    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final HiddenListingRepository hiddenListingRepository = mock(HiddenListingRepository.class);
    private final ListingEventPublisher listingEventPublisher = mock(ListingEventPublisher.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final ListingCleanupParticipant participant = new ListingCleanupParticipant("listing-ms", listingRepository, favoriteRepository, hiddenListingRepository, listingEventPublisher, transactionTemplate, Duration.ofDays(7), 20000);

    @BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    @Test
    void deletesFavoritesHiddenAndListingsOfStaleListings() {
        List<Long> stale = List.of(1L, 2L, 3L);
        when(listingRepository.findStaleListingIds(eq(MIDNIGHT.minus(Duration.ofDays(7))), any())).thenReturn(stale);
        when(favoriteRepository.deleteByListingIdIn(stale)).thenReturn(2);
        when(hiddenListingRepository.deleteByListingIdIn(stale)).thenReturn(1);
        when(listingRepository.deleteByIdIn(stale)).thenThrow(new IllegalStateException("lock timeout"));

        CleanupResult result = participant.cleanUp(MIDNIGHT);

        assertThat(result.deleted()).isEqualTo(3);
        assertThat(result.failed()).isEqualTo(3);
        assertThat(result.errors()).isEmpty();
        verify(favoriteRepository).deleteByListingIdIn(stale);
        verify(hiddenListingRepository).deleteByListingIdIn(stale);
    }

    @Test
    void publishesDeletedListingsInTheSameTransaction() {
        List<Long> stale = List.of(1L, 2L);
        when(listingRepository.findStaleListingIds(any(), any())).thenReturn(stale);
        when(listingRepository.deleteByIdIn(stale)).thenReturn(2);

        CleanupResult result = participant.cleanUp(MIDNIGHT);

        assertThat(result.deleted()).isEqualTo(2);
        verify(listingEventPublisher).publishListingsDeleted(stale);
    }

    @Test
    void reportsFailedSelection() {
        when(listingRepository.findStaleListingIds(any(), any())).thenThrow(new IllegalStateException("connection refused"));

        CleanupResult result = participant.cleanUp(MIDNIGHT);

        assertThat(result.errors()).containsExactly("listings: connection refused");
        verifyNoInteractions(favoriteRepository, hiddenListingRepository);
    }
}
