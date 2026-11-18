package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.listing.ListingDto;
import com.rentalert.internal.client.service.ListingMsService;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoommateListingCardsTests {

    private final ListingMsService listingMsService = mock(ListingMsService.class);
    private final RoommateListingCards cards =
        new RoommateListingCards(listingMsService, new RoommateBatchCache(new InMemoryCache().service()));

    @Test
    void repeatedLookupIsServedFromCache() {
        when(listingMsService.findCards(List.of(11L))).thenReturn(List.of(card(11L)));

        cards.find(List.of(11L));
        var second = cards.find(List.of(11L));

        assertThat(second).containsOnlyKeys(11L);
        assertThat(second.get(11L).getTitle()).isEqualTo("Flat 11");
        verify(listingMsService, times(1)).findCards(List.of(11L));
    }

    @Test
    void evictedCardIsLoadedAgain() {
        when(listingMsService.findCards(List.of(11L))).thenReturn(List.of(card(11L)));
        cards.find(List.of(11L));

        cards.evict(List.of(11L));
        cards.find(List.of(11L));

        verify(listingMsService, times(2)).findCards(List.of(11L));
    }

    private static ListingDto card(long id) {
        return ListingDto.builder().id(id).title("Flat " + id).build();
    }
}
