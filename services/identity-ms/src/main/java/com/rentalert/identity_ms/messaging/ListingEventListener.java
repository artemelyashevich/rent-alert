package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.identity_ms.service.NewListingNotificationService;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ListingEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "identity.listings";

    private final NewListingNotificationService newListingNotificationService;

    public ListingEventListener(NewListingNotificationService newListingNotificationService) {
        super(CONSUMER);
        this.newListingNotificationService = newListingNotificationService;
    }

    @KafkaListener(topics = KafkaTopics.LISTINGS, groupId = CONSUMER)
    public void onListingCreated(ListingCreatedEvent event) {
        if (event.listingId() == null) {
            throw new NonRetryableEventException("Listing event without listing id");
        }
        processOnce(listingKey(event), () -> newListingNotificationService.notifyMatchingUsers(event));
    }

    private static UUID listingKey(ListingCreatedEvent event) {
        return UUID.nameUUIDFromBytes(("ListingCreatedEvent:" + event.listingId()).getBytes(StandardCharsets.UTF_8));
    }
}
