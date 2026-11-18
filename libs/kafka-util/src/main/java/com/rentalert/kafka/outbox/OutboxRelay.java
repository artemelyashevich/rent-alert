package com.rentalert.kafka.outbox;

import com.rentalert.dto.GlobalConstants;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class OutboxRelay {

    private static final String SELECT_BATCH = """
        SELECT id, topic, message_key, payload, trace_id, attempts FROM outbox_event
        WHERE published_at IS NULL
        ORDER BY id
        LIMIT ?
        FOR UPDATE SKIP LOCKED
        """;
    private static final String TRY_RELAY_LOCK = "SELECT pg_try_advisory_xact_lock(?)";
    private static final long RELAY_LOCK_KEY = 4_218_765_031L;
    private static final String MARK_PUBLISHED = "UPDATE outbox_event SET published_at = ? WHERE id = ?";
    private static final String MARK_FAILED = "UPDATE outbox_event SET attempts = attempts + 1, last_error = ? WHERE id = ?";
    private static final String DELETE_PUBLISHED = "DELETE FROM outbox_event WHERE published_at < ?";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final int batchSize;
    private final Duration sendTimeout;
    private final Duration retention;

    public OutboxRelay(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate,
        KafkaTemplate<String, Object> kafkaTemplate, JsonMapper jsonMapper,
        int batchSize, Duration sendTimeout, Duration retention) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.batchSize = batchSize;
        this.sendTimeout = sendTimeout;
        this.retention = retention;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.relay-interval:PT1S}")
    public void relay() {
        Integer sent;
        do {
            sent = transactionTemplate.execute(status -> relayBatch());
        } while (sent != null && sent == batchSize);
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.cleanup-interval:PT1H}")
    public void deletePublished() {
        int deleted = jdbcTemplate.update(DELETE_PUBLISHED, Timestamp.from(Instant.now().minus(retention)));
        if (deleted > 0) {
            log.debug("Deleted {} published outbox rows", deleted);
        }
    }

    // Stops at the first failure: sending later rows would reorder messages of the same key
    private int relayBatch() {
        if (!tryAcquireRelayLock()) {
            return 0;
        }
        List<OutboxRow> rows = jdbcTemplate.query(SELECT_BATCH, (rs, i) -> new OutboxRow(
            rs.getLong("id"), rs.getString("topic"), rs.getString("message_key"), rs.getString("payload"),
            rs.getString("trace_id"), rs.getInt("attempts")), batchSize);
        int sent = 0;
        long lastId = 0;
        for (OutboxRow row : rows) {
            try {
                kafkaTemplate.send(toRecord(row)).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Failed to relay outbox row {} to {} attempts={}: {}", row.id(), row.topic(), row.attempts() + 1, e.getMessage());
                jdbcTemplate.update(MARK_FAILED, String.valueOf(e.getMessage()), row.id());
                return -1;
            }
            jdbcTemplate.update(MARK_PUBLISHED, Timestamp.from(Instant.now()), row.id());
            log.debug("outbox relayed id={} topic={} key={}", row.id(), row.topic(), row.key());
            sent++;
            lastId = row.id();
        }
        if (sent > 0) {
            log.info("outbox relayed count={} lastId={}", sent, lastId);
        }
        return sent;
    }

    private boolean tryAcquireRelayLock() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(TRY_RELAY_LOCK, Boolean.class, RELAY_LOCK_KEY));
    }

    private ProducerRecord<String, Object> toRecord(OutboxRow row) {
        ProducerRecord<String, Object> record =
            new ProducerRecord<>(row.topic(), row.key(), jsonMapper.readTree(row.payload()));
        if (row.traceId() != null) {
            record.headers().add(GlobalConstants.TRACE_ID_HEADER, row.traceId().getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    private record OutboxRow(long id, String topic, String key, String payload, String traceId, int attempts) {

    }
}
