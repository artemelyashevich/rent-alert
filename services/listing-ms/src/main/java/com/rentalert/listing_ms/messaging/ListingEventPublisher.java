package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingsDeletedEvent;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListingEventPublisher extends AbstractOutboxEventPublisher {

    private static final String KEY = "listing-ms";

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishListingsDeleted(List<Long> listingIds) {
        publish(KafkaTopics.LISTINGS_DELETED, KEY, new ListingsDeletedEvent(listingIds));
    }
}
