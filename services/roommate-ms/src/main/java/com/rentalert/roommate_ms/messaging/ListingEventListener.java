package com.rentalert.roommate_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingsDeletedEvent;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.roommate_ms.service.RoommateListingCleanupService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ListingEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "roommate.listings-deleted";

    private final RoommateListingCleanupService listingCleanupService;

    public ListingEventListener(RoommateListingCleanupService listingCleanupService) {
        super(CONSUMER);
        this.listingCleanupService = listingCleanupService;
    }

    /**
     * Drops shared favorites of deleted listings and clears their id in roommate listings and viewings.
     */
    @KafkaListener(topics = KafkaTopics.LISTINGS_DELETED, groupId = CONSUMER)
    public void onListingsDeleted(ListingsDeletedEvent event) {
        processOnce(keyOf(event), () -> listingCleanupService.deleteListingReferences(event.listingIds()));
    }

    private static UUID keyOf(ListingsDeletedEvent event) {
        return UUID.nameUUIDFromBytes(("ListingsDeletedEvent:" + event.listingIds()).getBytes(StandardCharsets.UTF_8));
    }
}
