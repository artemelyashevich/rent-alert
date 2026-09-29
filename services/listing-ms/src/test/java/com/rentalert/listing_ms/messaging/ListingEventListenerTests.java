package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.listing_ms.service.ListingService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ListingEventListenerTests {

    private final ListingService listingService = mock(ListingService.class);
    private final ListingEventListener listener = new ListingEventListener(listingService);

    @Test
    void storesValidEvent() {
        ListingCreatedEvent event = ListingCreatedEvent.builder()
            .listingId(7L)
            .source(ScheduleProvider.REALT)
            .sourceId("uuid")
            .build();

        listener.onListingCreated(event);

        verify(listingService).saveCreated(event);
    }

    @Test
    void eventWithoutIdIsNotRetried() {
        ListingCreatedEvent event = ListingCreatedEvent.builder().source(ScheduleProvider.REALT).sourceId("uuid").build();

        assertThatThrownBy(() -> listener.onListingCreated(event))
            .isInstanceOf(NonRetryableEventException.class);
        verify(listingService, never()).saveCreated(any());
    }
}
