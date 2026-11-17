package com.rentalert.processing_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Component
public class ListingEventPublisher extends AbstractOutboxEventPublisher {

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishListingCreated(ListingCreatedEvent event) {
        publish(KafkaTopics.LISTINGS, String.valueOf(event.listingId()), event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishNotificationRequest(NotificationRequest request) {
        publish(KafkaTopics.notificationRequests(request.type()), request.deduplicationKey(), request);
    }
}
