package com.rentalert.notification_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingsDeletedEvent;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.notification_ms.service.NotificationService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ListingEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "notification.listings-deleted";

    private final NotificationService notificationService;

    public ListingEventListener(NotificationService notificationService) {
        super(CONSUMER);
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.LISTINGS_DELETED, groupId = CONSUMER)
    public void onListingsDeleted(ListingsDeletedEvent event) {
        processOnce(keyOf(event), () -> notificationService.deleteByListingIds(event.listingIds()));
    }

    private static UUID keyOf(ListingsDeletedEvent event) {
        return UUID.nameUUIDFromBytes(("ListingsDeletedEvent:" + event.listingIds()).getBytes(StandardCharsets.UTF_8));
    }
}
