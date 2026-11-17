package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.listing_ms.service.ListingService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ListingEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "listing.listings";

    private final ListingService listingService;

    public ListingEventListener(ListingService listingService) {
        super(CONSUMER);
        this.listingService = listingService;
    }

    /**
     * Stores a created listing once per event. The event carries no idempotency key, so the key is derived from the
     * event type, listing id and source identity, which are the same on every redelivery.
     */
    @KafkaListener(topics = KafkaTopics.LISTINGS, groupId = CONSUMER)
    public void onListingCreated(ListingCreatedEvent event) {
        if (event.listingId() == null || event.source() == null || event.sourceId() == null) {
            throw new NonRetryableEventException("Listing event without listing id, source or source id");
        }
        processOnce(listingCreatedKey(event), () -> listingService.saveCreated(event));
    }

    private static UUID listingCreatedKey(ListingCreatedEvent event) {
        String seed = "ListingCreatedEvent:" + event.listingId() + ":" + event.source() + ":" + event.sourceId();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
