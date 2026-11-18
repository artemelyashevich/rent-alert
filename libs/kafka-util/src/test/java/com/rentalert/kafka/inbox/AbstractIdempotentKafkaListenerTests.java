package com.rentalert.kafka.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rentalert.kafka.NonRetryableEventException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

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
        logger = (Logger) LoggerFactory.getLogger(AbstractIdempotentKafkaListener.class);
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(null);
        MDC.clear();
        jdbcTemplate.execute("SHUTDOWN");
    }

    @Test
    void processedEventIsLoggedAtDebug() {
        UUID key = UUID.randomUUID();

        listener.handle(key, () -> { });

        assertThat(logged(Level.DEBUG)).containsExactly("event processed consumer=roommate.test-listener key=" + key);
    }

    @Test
    void duplicateEventIsLoggedAtInfo() {
        UUID key = UUID.randomUUID();
        listener.handle(key, () -> { });

        listener.handle(key, () -> { });

        assertThat(logged(Level.INFO)).containsExactly("event duplicate skipped consumer=roommate.test-listener key=" + key);
    }

    @Test
    void failedEventIsLoggedAtWarnWithExceptionClassAndMessage() {
        UUID key = UUID.randomUUID();

        assertThatThrownBy(() -> listener.handle(key, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(logged(Level.WARN)).containsExactly("event failed consumer=roommate.test-listener key=" + key
            + " error=java.lang.IllegalStateException: boom");
    }

    @Test
    void rejectedEventIsLoggedAtWarnWithReason() {
        assertThatThrownBy(() -> listener.handle(null, () -> { }))
            .isInstanceOf(NonRetryableEventException.class);

        assertThat(logged(Level.WARN)).containsExactly(
            "event rejected consumer=roommate.test-listener reason=Event without idempotency key for consumer roommate.test-listener");
    }

    @Test
    void mdcIsSetDuringHandlingAndClearedAfterSuccessOrFailure() {
        UUID key = UUID.randomUUID();
        List<String> seen = new ArrayList<>();

        listener.handle(key, () -> seen.add(MDC.get("consumer") + "|" + MDC.get("idempotencyKey")));

        assertThat(seen).containsExactly("roommate.test-listener|" + key);
        assertThat(MDC.get("consumer")).isNull();
        assertThat(MDC.get("idempotencyKey")).isNull();

        assertThatThrownBy(() -> listener.handle(UUID.randomUUID(), () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get("consumer")).isNull();
        assertThat(MDC.get("idempotencyKey")).isNull();
    }

    @Test
    void payloadValuesAreNotLogged() {
        String marker = "PAYLOAD-MARKER-7f3a9c";
        UUID key = UUID.randomUUID();

        listener.handle(key, () -> marker.length());
        listener.handle(key, () -> marker.length());
        assertThatThrownBy(() -> listener.handle(UUID.randomUUID(), () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> listener.handle(null, () -> marker.length()))
            .isInstanceOf(NonRetryableEventException.class);

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list.stream().map(ILoggingEvent::getFormattedMessage))
            .noneMatch(message -> message.contains(marker));
    }

    private List<String> logged(Level level) {
        return appender.list.stream()
            .filter(event -> event.getLevel() == level)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();
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
