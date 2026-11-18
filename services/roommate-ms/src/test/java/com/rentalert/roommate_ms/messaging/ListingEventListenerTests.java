package com.rentalert.roommate_ms.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.listing.ListingsDeletedEvent;
import com.rentalert.roommate_ms.service.RoommateListingCards;
import com.rentalert.roommate_ms.service.RoommateListingCleanupService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ListingEventListenerTests {

    private final RoommateListingCards listingCards = mock(RoommateListingCards.class);
    private final RoommateListingCleanupService cleanupService = mock(RoommateListingCleanupService.class);
    private final ListingEventListener listener = createListener();

    private ListingEventListener createListener() {
        ListingEventListener created = new ListingEventListener(listingCards, cleanupService);
        ReflectionTestUtils.setField(created, "inboxGuard", new InMemoryInboxGuard());
        return created;
    }

    @Test
    void deletedListingIdsAreForwardedToCleanup() {
        listener.onListingsDeleted(new ListingsDeletedEvent(List.of(1L, 2L)));

        verify(cleanupService).deleteListingReferences(List.of(1L, 2L));
    }

    @Test
    void deletedListingsAreEvictedFromCardCache() {
        listener.onListingsDeleted(new ListingsDeletedEvent(List.of(1L, 2L)));

        verify(listingCards).evict(List.of(1L, 2L));
    }

    @Test
    void redeliveredEventCleansUpOnlyOnce() {
        listener.onListingsDeleted(new ListingsDeletedEvent(List.of(1L, 2L)));
        listener.onListingsDeleted(new ListingsDeletedEvent(List.of(1L, 2L)));

        verify(cleanupService, times(1)).deleteListingReferences(List.of(1L, 2L));
    }
}
