package com.rentalert.kafka.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;


public class OutboxPublisher {

    private static final String INSERT = "INSERT INTO outbox_event (topic, message_key, payload) VALUES (?, ?, ?)";

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
        jdbcTemplate.update(INSERT, topic, key, jsonMapper.writeValueAsString(payload));
    }
}
