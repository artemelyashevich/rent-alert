package com.rentalert.kafka.inbox;

import com.rentalert.kafka.NonRetryableEventException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractIdempotentKafkaListener {

    private final String consumerName;

    @Autowired(required = false)
    private InboxGuard inboxGuard;

    protected AbstractIdempotentKafkaListener(String consumerName) {
        this.consumerName = consumerName;
    }

    protected void processOnce(UUID idempotencyKey, Runnable handler) {
        if (idempotencyKey == null) {
            throw new NonRetryableEventException("Event without idempotency key for consumer " + consumerName);
        }
        if (inboxGuard == null) {
            throw new IllegalStateException("InboxGuard bean is missing for consumer " + consumerName
                + ", enable it with app.kafka.inbox.enabled=true");
        }
        inboxGuard.processOnce(consumerName, idempotencyKey, handler);
    }
}
