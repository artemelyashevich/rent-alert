package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.listing_ms.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListingEventListener {

    private final ListingService listingService;

    @KafkaListener(topics = KafkaTopics.LISTINGS, groupId = "listing.listings")
    public void onListingCreated(ListingCreatedEvent event) {
        if (event.listingId() == null || event.source() == null || event.sourceId() == null) {
            throw new NonRetryableEventException("Listing event without listing id, source or source id");
        }
        listingService.saveCreated(event);
    }
}
