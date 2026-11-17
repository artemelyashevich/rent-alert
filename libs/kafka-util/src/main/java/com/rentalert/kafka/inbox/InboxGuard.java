package com.rentalert.kafka.inbox;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Idempotent processing of consumed events: the handler runs at most once per (consumer, idempotency key).
 * The marker row and the handler share one transaction, so a failed handler leaves no marker and the
 * redelivered event is processed again. Rows older than the retention are deleted, so a redelivery after
 * the retention window is processed again.
 */
@Slf4j
public class InboxGuard {

    private static final String INSERT =
        "INSERT INTO inbox_event (consumer, idempotency_key) VALUES (?, ?) ON CONFLICT DO NOTHING";
    private static final String DELETE_PROCESSED = "DELETE FROM inbox_event WHERE processed_at < ?";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Duration retention;

    public InboxGuard(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate, Duration retention) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.retention = retention;
    }

    /**
     * Runs the handler unless the event was already processed by this consumer. Exceptions from the handler
     * roll back the marker and propagate, so the listener error handler can retry the record.
     *
     * @return {@code true} if the handler ran, {@code false} for a duplicate
     */
    public boolean processOnce(String consumer, UUID idempotencyKey, Runnable handler) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            if (jdbcTemplate.update(INSERT, consumer, idempotencyKey) == 0) {
                log.debug("Skipping duplicate event {} for consumer {}", idempotencyKey, consumer);
                return false;
            }
            handler.run();
            return true;
        }));
    }

    @Scheduled(fixedDelayString = "${app.kafka.inbox.cleanup-interval:PT1H}")
    public void deleteProcessed() {
        int deleted = jdbcTemplate.update(DELETE_PROCESSED, Timestamp.from(Instant.now().minus(retention)));
        if (deleted > 0) {
            log.debug("Deleted {} processed inbox rows", deleted);
        }
    }
}
