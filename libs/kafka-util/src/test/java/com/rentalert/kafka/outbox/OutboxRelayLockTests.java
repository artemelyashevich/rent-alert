package com.rentalert.kafka.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

class OutboxRelayLockTests {

    private static final int BATCH_SIZE = 10;

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final OutboxRelay relay = new OutboxRelay(jdbcTemplate, transactionTemplate, kafkaTemplate,
        JsonMapper.builder().build(), BATCH_SIZE, Duration.ofSeconds(1), Duration.ofDays(7));

    @Test
    void relaySendsNothingWhenRelayLockIsHeldByAnotherInstance() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyLong())).thenReturn(false);
        runTransactionsInline();

        relay.relay();

        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), anyInt());
        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        verify(jdbcTemplate, never()).update(anyString(), any(Timestamp.class), anyLong());
    }

    @Test
    void relaySendsRowsWhenRelayLockIsAcquired() throws SQLException {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), anyLong())).thenReturn(true);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).thenAnswer(invocation -> {
            RowMapper<?> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(outboxRow(), 0));
        });
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
        runTransactionsInline();

        relay.relay();

        verify(kafkaTemplate).send(any(ProducerRecord.class));
        verify(jdbcTemplate).update(anyString(), any(Timestamp.class), eq(7L));
    }

    private void runTransactionsInline() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Integer> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    private static ResultSet outboxRow() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(7L);
        when(rs.getString("topic")).thenReturn("outbox-topic");
        when(rs.getString("message_key")).thenReturn("42");
        when(rs.getString("payload")).thenReturn("{\"count\":3}");
        return rs;
    }
}
