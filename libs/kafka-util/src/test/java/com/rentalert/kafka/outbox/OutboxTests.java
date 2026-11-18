package com.rentalert.kafka.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rentalert.kafka.config.AbstractKafkaConsumerConfiguration;
import com.rentalert.kafka.config.AbstractKafkaProducerConfiguration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

class OutboxTests {

    private static final String TOPIC = "outbox-test";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static EmbeddedKafkaKraftBroker broker;
    // A Boot application converts "PT10S" for @Value itself; the bare runner needs Boot's conversion service
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withInitializer(context -> context.getBeanFactory()
            .setConversionService(ApplicationConversionService.getSharedInstance()))
        .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
        .withUserConfiguration(DatabaseConfiguration.class, TestProducerConfiguration.class,
            TestConsumerConfiguration.class)
        .withBean(JsonMapper.class, () -> JSON)
        .withBean(RawListener.class);

    @BeforeAll
    static void startBroker() {
        broker = new EmbeddedKafkaKraftBroker(1, 1, TOPIC);
        broker.afterPropertiesSet();
    }

    @AfterAll
    static void stopBroker() {
        broker.destroy();
    }

    private static ConsumerRecord<String, String> await(RawListener listener, String marker) throws InterruptedException {
        ConsumerRecord<String, String> record;
        do {
            record = listener.received.poll(30, TimeUnit.SECONDS);
        } while (record != null && !record.value().contains(marker));
        assertThat(record).as("record with %s", marker).isNotNull();
        return record;
    }

    private ApplicationContextRunner runner() {
        return runner.withPropertyValues(
            "spring.application.name=outbox-test",
            "spring.kafka.bootstrap-servers=" + broker.getBrokersAsString());
    }

    @Test
    void eventPublishedInCommittedTransactionIsRelayedWithKeyAndJson() {
        String marker = UUID.randomUUID().toString();

        runner().run(context -> {
            TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            tx.executeWithoutResult(status -> context.getBean(OutboxPublisher.class)
                .publish(TOPIC, "42", Map.of("marker", marker, "count", 3)));

            context.getBean(OutboxRelay.class).relay();

            ConsumerRecord<String, String> record = await(context.getBean(RawListener.class), marker);
            assertThat(record.key()).isEqualTo("42");
            assertThat(JSON.readTree(record.value())).isEqualTo(JSON.readTree(
                "{\"marker\":\"" + marker + "\",\"count\":3}"));
            assertThat(context.getBean(JdbcTemplate.class).queryForObject(
                "SELECT count(*) FROM outbox_event WHERE published_at IS NULL", Integer.class)).isZero();
        });
    }

    @Test
    void rolledBackTransactionPublishesNothing() {
        runner().run(context -> {
            TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            tx.executeWithoutResult(status -> {
                context.getBean(OutboxPublisher.class).publish(TOPIC, "42", Map.of("marker", "rolled-back"));
                status.setRollbackOnly();
            });

            assertThat(context.getBean(JdbcTemplate.class)
                .queryForObject("SELECT count(*) FROM outbox_event", Integer.class)).isZero();
        });
    }

    @Test
    void publishOutsideTransactionIsRejected() {
        runner().run(context -> assertThatThrownBy(() ->
            context.getBean(OutboxPublisher.class).publish(TOPIC, "42", Map.of()))
            .isInstanceOf(IllegalStateException.class));
    }

    @Test
    void outboxCanBeDisabled() {
        runner().withPropertyValues("app.kafka.outbox.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(OutboxPublisher.class);
            assertThat(context).doesNotHaveBean(OutboxRelay.class);
        });
    }

    @Configuration
    static class TestProducerConfiguration extends AbstractKafkaProducerConfiguration {

    }

    @Configuration
    static class TestConsumerConfiguration extends AbstractKafkaConsumerConfiguration {

    }

    // H2 lacks partial indexes, so the test schema is the table of db/outbox/outbox_event.sql only
    @Configuration
    static class DatabaseConfiguration {

        @Bean
        DataSource dataSource() {
            EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("classpath:outbox-test-schema.sql")
                .build();
            new JdbcTemplate(database).execute("CREATE ALIAS pg_try_advisory_xact_lock AS $$ "
                + "boolean tryLock(long key) { return true; } $$");
            return database;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    static class RawListener {

        final BlockingQueue<ConsumerRecord<String, String>> received = new LinkedBlockingQueue<>();

        @KafkaListener(topics = TOPIC)
        void on(ConsumerRecord<String, String> record) {
            received.add(record);
        }
    }
}
