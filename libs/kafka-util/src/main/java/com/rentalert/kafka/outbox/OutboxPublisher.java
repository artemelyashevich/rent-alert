package com.rentalert.kafka.outbox;

import com.rentalert.dto.GlobalConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class OutboxPublisher {

    private static final String INSERT =
        "INSERT INTO outbox_event (topic, message_key, payload, trace_id) VALUES (?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    public OutboxPublisher(JdbcTemplate jdbcTemplate, JsonMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    public void publish(String topic, String key, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("OutboxPublisher.publish must run inside the business transaction");
        }
        jdbcTemplate.update(INSERT, topic, key, jsonMapper.writeValueAsString(payload), MDC.get(GlobalConstants.TRACE_ID));
        log.debug("event queued topic={} key={} type={}", topic, key, payload.getClass().getSimpleName());
    }
}
