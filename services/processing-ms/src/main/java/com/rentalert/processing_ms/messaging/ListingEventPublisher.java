package com.rentalert.processing_ms.messaging;

import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.kafka.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class ListingEventPublisher {

    private final OutboxPublisher outboxPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishListingCreated(ListingCreatedEvent event) {
        outboxPublisher.publish(KafkaTopics.LISTINGS, String.valueOf(event.listingId()), event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishNotificationRequest(NotificationRequest request) {
        outboxPublisher.publish(KafkaTopics.notificationRequests(request.type()), request.deduplicationKey(), request);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishCurrencyRate(CurrencyPair pair, CurrencyRateDto rate) {
        outboxPublisher.publish(KafkaTopics.CURRENCY_RATES, pair.name(), rate);
    }
}
