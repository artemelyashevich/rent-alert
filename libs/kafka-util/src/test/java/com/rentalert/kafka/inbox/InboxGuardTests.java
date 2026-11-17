package com.rentalert.kafka.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class InboxGuardTests {

    private static final String CONSUMER = "roommate-ms.test-listener";

    private JdbcTemplate jdbcTemplate;
    private InboxGuard guard;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:inbox-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("inbox-test-schema.sql")).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        guard = new InboxGuard(jdbcTemplate,
            new TransactionTemplate(new DataSourceTransactionManager(dataSource)), Duration.ofDays(7));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SHUTDOWN");
    }

    @Test
    void duplicateEventIsNotHandledTwice() {
        UUID key = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();

        assertThat(guard.processOnce(CONSUMER, key, calls::incrementAndGet)).isTrue();
        assertThat(guard.processOnce(CONSUMER, key, calls::incrementAndGet)).isFalse();

        assertThat(calls).hasValue(1);
        assertThat(rowCount()).isOne();
    }

    @Test
    void failedHandlerRollsBackMarkerAndRedeliveryIsHandled() {
        UUID key = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> guard.processOnce(CONSUMER, key, () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(rowCount()).isZero();

        assertThat(guard.processOnce(CONSUMER, key, calls::incrementAndGet)).isTrue();

        assertThat(calls).hasValue(2);
        assertThat(rowCount()).isOne();
    }

    @Test
    void sameKeyForDifferentConsumersIsProcessedIndependently() {
        UUID key = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();

        assertThat(guard.processOnce("roommate-ms.first", key, calls::incrementAndGet)).isTrue();
        assertThat(guard.processOnce("roommate-ms.second", key, calls::incrementAndGet)).isTrue();

        assertThat(calls).hasValue(2);
        assertThat(rowCount()).isEqualTo(2);
    }

    @Test
    void deleteProcessedRemovesRowsOlderThanRetentionOnly() {
        jdbcTemplate.update("INSERT INTO inbox_event (consumer, idempotency_key, processed_at) VALUES (?, ?, ?)",
            CONSUMER, UUID.randomUUID(), Timestamp.from(Instant.now().minus(Duration.ofDays(8))));
        UUID freshKey = UUID.randomUUID();
        guard.processOnce(CONSUMER, freshKey, () -> { });

        guard.deleteProcessed();

        assertThat(rowCount()).isOne();
        assertThat(guard.processOnce(CONSUMER, freshKey, () -> { })).isFalse();
    }

    private Integer rowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM inbox_event", Integer.class);
    }
}
