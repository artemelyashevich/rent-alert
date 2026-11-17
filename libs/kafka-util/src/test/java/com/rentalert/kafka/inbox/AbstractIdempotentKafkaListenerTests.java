package com.rentalert.kafka.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rentalert.kafka.NonRetryableEventException;
import java.time.Duration;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

class AbstractIdempotentKafkaListenerTests {

    private JdbcTemplate jdbcTemplate;
    private InboxGuard guard;
    private TestListener listener;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:idempotent-listener-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("inbox-test-schema.sql")).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        guard = new InboxGuard(jdbcTemplate,
            new TransactionTemplate(new DataSourceTransactionManager(dataSource)), Duration.ofDays(7));
        listener = new TestListener("roommate.test-listener");
        ReflectionTestUtils.setField(listener, "inboxGuard", guard);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SHUTDOWN");
    }

    @Test
    void duplicateEventIsNotHandledTwice() {
        UUID key = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();

        listener.handle(key, calls::incrementAndGet);
        listener.handle(key, calls::incrementAndGet);

        assertThat(calls).hasValue(1);
        assertThat(rowCount()).isOne();
    }

    @Test
    void nullKeyIsNonRetryable() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> listener.handle(null, calls::incrementAndGet))
            .isInstanceOf(NonRetryableEventException.class);

        assertThat(calls).hasValue(0);
        assertThat(rowCount()).isZero();
    }

    @Test
    void failedHandlerRollsBackAndExceptionPropagates() {
        UUID key = UUID.randomUUID();

        assertThatThrownBy(() -> listener.handle(key, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(rowCount()).isZero();
    }

    @Test
    void listenersWithDifferentConsumerNamesAreIndependent() {
        UUID key = UUID.randomUUID();
        TestListener other = new TestListener("roommate.other-listener");
        ReflectionTestUtils.setField(other, "inboxGuard", guard);
        AtomicInteger calls = new AtomicInteger();

        listener.handle(key, calls::incrementAndGet);
        other.handle(key, calls::incrementAndGet);

        assertThat(calls).hasValue(2);
        assertThat(rowCount()).isEqualTo(2);
    }

    @Test
    void missingInboxGuardFailsWithClearMessage() {
        TestListener withoutInbox = new TestListener("roommate.no-inbox");

        assertThatThrownBy(() -> withoutInbox.handle(UUID.randomUUID(), () -> { }))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("app.kafka.inbox.enabled");
    }

    private Integer rowCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM inbox_event", Integer.class);
    }

    static class TestListener extends AbstractIdempotentKafkaListener {

        TestListener(String consumerName) {
            super(consumerName);
        }

        void handle(UUID key, Runnable handler) {
            processOnce(key, handler);
        }
    }
}
