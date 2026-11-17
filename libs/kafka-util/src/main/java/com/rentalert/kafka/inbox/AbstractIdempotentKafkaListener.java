package com.rentalert.kafka.inbox;

import com.rentalert.kafka.NonRetryableEventException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for Kafka listeners whose handlers must run at most once per event. Subclasses declare their own
 * {@code @KafkaListener} method and call {@link #processOnce} from it; the {@link InboxGuard} is injected here.
 * Requires {@code app.kafka.inbox.enabled=true}, otherwise {@link #processOnce} fails with {@link IllegalStateException}.
 */
public abstract class AbstractIdempotentKafkaListener {

    private final String consumerName;

    @Autowired(required = false)
    private InboxGuard inboxGuard;

    /**
     * @param consumerName stable listener name used as the inbox consumer key; must not change between releases
     */
    protected AbstractIdempotentKafkaListener(String consumerName) {
        this.consumerName = consumerName;
    }

    /**
     * Runs the handler unless this consumer already processed the event. A {@code null} key is not retryable and
     * is rejected with {@link NonRetryableEventException}.
     */
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
