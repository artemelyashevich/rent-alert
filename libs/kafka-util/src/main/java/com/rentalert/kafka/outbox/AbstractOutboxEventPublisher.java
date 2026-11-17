package com.rentalert.kafka.outbox;

import com.rentalert.dto.event.AbstractEvent;
import com.rentalert.dto.event.EventStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for service publishers that write events to the transactional outbox. The {@link OutboxPublisher} bean is
 * injected here, so subclasses do not inject it. Subclass methods must run inside the business transaction: this class
 * does not open one, and {@link OutboxPublisher#publish} fails with {@link IllegalStateException} without an active
 * transaction.
 */
public abstract class AbstractOutboxEventPublisher {

    @Autowired
    private OutboxPublisher outboxPublisher;

    /**
     * Stores the payload in the outbox of the current transaction; it is relayed to Kafka only after that transaction commits.
     */
    protected void publish(String topic, String key, Object payload) {
        outboxPublisher.publish(topic, key, payload);
    }

    /**
     * Publishes a user-scoped event with {@code initiatorId} set to {@code userId}, {@code SUCCESS} status unless already
     * set, and the key {@code String.valueOf(userId)}.
     */
    protected void publishEvent(String topic, Long userId, AbstractEvent event) {
        event.setInitiatorId(userId);
        if (event.getEventStatus() == null) {
            event.setEventStatus(EventStatus.SUCCESS);
        }
        publish(topic, String.valueOf(userId), event);
    }
}
