package com.rentalert.kafka.inbox;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

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
