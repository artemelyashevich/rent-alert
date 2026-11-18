package com.rentalert.kafka.outbox;

import com.rentalert.dto.event.AbstractEvent;
import com.rentalert.dto.event.EventStatus;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractOutboxEventPublisher {

    @Autowired
    private OutboxPublisher outboxPublisher;

    protected void publish(String topic, String key, Object payload) {
        outboxPublisher.publish(topic, key, payload);
    }

    protected void publishEvent(String topic, Long userId, AbstractEvent event) {
        event.setInitiatorId(userId);
        if (event.getEventStatus() == null) {
            event.setEventStatus(EventStatus.SUCCESS);
        }
        publish(topic, String.valueOf(userId), event);
    }
}
