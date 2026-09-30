package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.identity_ms.service.NewListingNotificationService;
import com.rentalert.kafka.NonRetryableEventException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListingEventListener {

    private final NewListingNotificationService newListingNotificationService;

    @KafkaListener(topics = KafkaTopics.LISTINGS, groupId = "identity.listings")
    public void onListingCreated(ListingCreatedEvent event) {
        if (event.listingId() == null) {
            throw new NonRetryableEventException("Listing event without listing id");
        }
        newListingNotificationService.notifyMatchingUsers(event);
    }
}
