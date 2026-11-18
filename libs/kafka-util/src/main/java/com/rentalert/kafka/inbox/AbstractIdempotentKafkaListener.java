package com.rentalert.kafka.inbox;

import com.rentalert.kafka.NonRetryableEventException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractIdempotentKafkaListener {

    private static final String CONSUMER_MDC_KEY = "consumer";
    private static final String IDEMPOTENCY_KEY_MDC_KEY = "idempotencyKey";

    private final String consumerName;

    @Autowired(required = false)
    private InboxGuard inboxGuard;

    protected AbstractIdempotentKafkaListener(String consumerName) {
        this.consumerName = consumerName;
    }

    protected void processOnce(UUID idempotencyKey, Runnable handler) {
        MDC.put(CONSUMER_MDC_KEY, consumerName);
        MDC.put(IDEMPOTENCY_KEY_MDC_KEY, String.valueOf(idempotencyKey));
        try {
            if (idempotencyKey == null) {
                throw new NonRetryableEventException("Event without idempotency key for consumer " + consumerName);
            }
            if (inboxGuard == null) {
                throw new IllegalStateException("InboxGuard bean is missing for consumer " + consumerName
                    + ", enable it with app.kafka.inbox.enabled=true");
            }
            if (inboxGuard.processOnce(consumerName, idempotencyKey, handler)) {
                log.debug("event processed consumer={} key={}", consumerName, idempotencyKey);
            } else {
                log.info("event duplicate skipped consumer={} key={}", consumerName, idempotencyKey);
            }
        } catch (NonRetryableEventException e) {
            log.warn("event rejected consumer={} reason={}", consumerName, e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.warn("event failed consumer={} key={} error={}", consumerName, idempotencyKey,
                e.getClass().getName() + ": " + e.getMessage());
            throw e;
        } finally {
            MDC.remove(CONSUMER_MDC_KEY);
            MDC.remove(IDEMPOTENCY_KEY_MDC_KEY);
        }
    }
}
