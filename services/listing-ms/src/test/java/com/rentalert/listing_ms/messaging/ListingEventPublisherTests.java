package com.rentalert.listing_ms.messaging;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingsDeletedEvent;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ListingEventPublisherTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final ListingEventPublisher publisher = createPublisher();

    private ListingEventPublisher createPublisher() {
        ListingEventPublisher created = new ListingEventPublisher();
        ReflectionTestUtils.setField(created, "outboxPublisher", outboxPublisher);
        return created;
    }

    @Test
    void publishesDeletedListingIds() {
        publisher.publishListingsDeleted(List.of(1L, 2L));

        verify(outboxPublisher).publish(eq(KafkaTopics.LISTINGS_DELETED), eq("listing-ms"), argThat((ListingsDeletedEvent event) ->
            event.listingIds().equals(List.of(1L, 2L))));
    }
}
