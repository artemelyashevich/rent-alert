package com.rentalert.listing_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.listing_ms.service.ListingService;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ListingEventListenerTests {

    private final ListingService listingService = mock(ListingService.class);
    private final InboxGuard inboxGuard = mock(InboxGuard.class);
    private final Set<String> processed = new HashSet<>();
    private final ListingEventListener listener = createListener();

    private ListingEventListener createListener() {
        ListingEventListener created = new ListingEventListener(listingService);
        ReflectionTestUtils.setField(created, "inboxGuard", inboxGuard);
        return created;
    }

    @BeforeEach
    void runHandlerOncePerKey() {
        doAnswer(invocation -> {
            if (!processed.add(invocation.getArgument(0) + ":" + invocation.getArgument(1))) {
                return false;
            }
            invocation.<Runnable>getArgument(2).run();
            return true;
        }).when(inboxGuard).processOnce(any(), any(), any(Runnable.class));
    }

    @Test
    void storesValidEvent() {
        ListingCreatedEvent event = createdEvent(7L, "uuid");

        listener.onListingCreated(event);

        verify(listingService).saveCreated(event);
    }

    @Test
    void redeliveredEventIsStoredOnce() {
        ListingCreatedEvent event = createdEvent(7L, "uuid");

        listener.onListingCreated(event);
        listener.onListingCreated(event);

        verify(listingService, times(1)).saveCreated(event);
    }

    @Test
    void differentListingsAreStoredSeparately() {
        ListingCreatedEvent first = createdEvent(7L, "uuid");
        ListingCreatedEvent second = createdEvent(8L, "uuid-2");

        listener.onListingCreated(first);
        listener.onListingCreated(second);

        verify(listingService).saveCreated(first);
        verify(listingService).saveCreated(second);
    }

    @Test
    void eventWithoutIdIsNotRetried() {
        ListingCreatedEvent event = ListingCreatedEvent.builder().source(ScheduleProvider.REALT).sourceId("uuid").build();

        assertThatThrownBy(() -> listener.onListingCreated(event))
            .isInstanceOf(NonRetryableEventException.class);
        verify(listingService, never()).saveCreated(any());
    }

    private ListingCreatedEvent createdEvent(Long listingId, String sourceId) {
        return ListingCreatedEvent.builder()
            .listingId(listingId)
            .source(ScheduleProvider.REALT)
            .sourceId(sourceId)
            .build();
    }
}
