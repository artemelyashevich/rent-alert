package com.rentalert.kafka.config;

import com.rentalert.kafka.NonRetryableEventException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigurationTests {

    private static final String TOPIC = "admin-actions";
    private static final String DLT = TOPIC + AbstractKafkaConsumerConfiguration.DLT_SUFFIX;
    private static final String FAILING = "FAILING";
    private static final String INVALID = "INVALID";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static EmbeddedKafkaKraftBroker broker;

    record TestEvent(UUID idempotencyKey, String name, List<String> params) {
        TestEvent(String name, List<String> params) {
            this(UUID.randomUUID(), name, params);
        }
    }

    @Configuration
    static class TestConsumerConfiguration extends AbstractKafkaConsumerConfiguration {
        @Override
        protected BackOff backOff() {
            return new FixedBackOff(0L, 1L);
        }

        // Only the test that sets the name creates it: Kafka can't shrink a topic created by another test
        @Bean
        KafkaAdmin.NewTopics createdTopics(@Value("${test.created-topic:}") String name) {
            return name.isEmpty() ? new KafkaAdmin.NewTopics() : topicWithDeadLetter(name, Duration.ofDays(1));
        }
    }

    @Configuration
    static class TestProducerConfiguration extends AbstractKafkaProducerConfiguration {
    }

    static class Listener {
        final BlockingQueue<TestEvent> received = new LinkedBlockingQueue<>();
        final AtomicInteger invalidAttempts = new AtomicInteger();

        @KafkaListener(topics = TOPIC)
        void onEvent(TestEvent event) {
            if (INVALID.equals(event.name())) {
                invalidAttempts.incrementAndGet();
                throw new NonRetryableEventException("invalid event");
            }
            if (FAILING.equals(event.name())) {
                throw new IllegalStateException("listener failure");
            }
            received.add(event);
        }
    }

    static class DeadLetterListener {
        final BlockingQueue<ConsumerRecord<String, String>> received = new LinkedBlockingQueue<>();

        @KafkaListener(topics = DLT, groupId = "dlt-reader")
        void onDeadLetter(ConsumerRecord<String, String> record) {
            received.add(record);
        }
    }

    @BeforeAll
    static void startBroker() {
        broker = new EmbeddedKafkaKraftBroker(1, 1, TOPIC, DLT);
        broker.afterPropertiesSet();
    }

    @AfterAll
    static void stopBroker() {
        broker.destroy();
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
        .withUserConfiguration(TestConsumerConfiguration.class, TestProducerConfiguration.class)
        .withBean(JsonMapper.class, () -> JSON)
        .withBean(Listener.class)
        .withBean(DeadLetterListener.class);

    private ApplicationContextRunner runner() {
        return runner.withPropertyValues(
            "spring.application.name=test-service",
            "spring.kafka.bootstrap-servers=" + broker.getBrokersAsString());
    }

    @Test
    void eventSentByProducerReachesListenerAsItsParameterType() {
        TestEvent sent = new TestEvent("CITY_DELETE", List.of("42"));

        runner().run(context -> {
            template(context).send(TOPIC, sent).get(10, TimeUnit.SECONDS);

            assertThat(awaitEvent(context.getBean(Listener.class), sent.idempotencyKey())).isEqualTo(sent);
        });
    }

    @Test
    void malformedRecordGoesToDeadLetterTopicAndDoesNotBlockTheNextOne() {
        String malformed = "not an event " + UUID.randomUUID();
        TestEvent valid = new TestEvent("JOB_CLEAN_UP", List.of());

        runner().run(context -> {
            KafkaTemplate<String, Object> template = template(context);
            template.send(TOPIC, malformed).get(10, TimeUnit.SECONDS);
            template.send(TOPIC, valid).get(10, TimeUnit.SECONDS);

            assertThat(awaitEvent(context.getBean(Listener.class), valid.idempotencyKey())).isNotNull();
            ConsumerRecord<String, String> deadLetter = awaitDeadLetter(context.getBean(DeadLetterListener.class), malformed);
            assertThat(header(deadLetter, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(TOPIC);
        });
    }

    @Test
    void recordStillFailingAfterRetriesGoesToDeadLetterTopicUnchanged() {
        TestEvent failing = new TestEvent(FAILING, List.of("1"));

        runner().run(context -> {
            template(context).send(TOPIC, failing).get(10, TimeUnit.SECONDS);

            ConsumerRecord<String, String> deadLetter =
                awaitDeadLetter(context.getBean(DeadLetterListener.class), failing.idempotencyKey().toString());
            assertThat(JSON.readValue(deadLetter.value(), TestEvent.class)).isEqualTo(failing);
            assertThat(header(deadLetter, KafkaHeaders.DLT_EXCEPTION_MESSAGE)).contains("listener failure");
        });
    }

    @Test
    void nonRetryableFailureGoesToDeadLetterTopicWithoutRetries() {
        TestEvent invalid = new TestEvent(INVALID, List.of());

        runner().run(context -> {
            template(context).send(TOPIC, invalid).get(10, TimeUnit.SECONDS);

            ConsumerRecord<String, String> deadLetter =
                awaitDeadLetter(context.getBean(DeadLetterListener.class), invalid.idempotencyKey().toString());
            assertThat(header(deadLetter, KafkaHeaders.DLT_EXCEPTION_MESSAGE)).contains("invalid event");
            assertThat(context.getBean(Listener.class).invalidAttempts).hasValue(1);
        });
    }

    @Test
    void topicAndDeadLetterTopicAreCreatedWithConfiguredPartitions() {
        String createdTopic = "created-" + UUID.randomUUID();

        runner().withPropertyValues("app.kafka.topic.partitions=2", "test.created-topic=" + createdTopic).run(context -> {
            Map<String, TopicDescription> topics = context.getBean(KafkaAdmin.class)
                .describeTopics(createdTopic, createdTopic + AbstractKafkaConsumerConfiguration.DLT_SUFFIX);
            assertThat(topics).hasSize(2);
            assertThat(topics.values()).allSatisfy(topic -> assertThat(topic.partitions()).hasSize(2));
        });
    }

    @Test
    void listenerPropertiesAreApplied() {
        runner().withPropertyValues("spring.kafka.listener.ack-mode=manual_immediate").run(context -> {
            ConcurrentKafkaListenerContainerFactory<?, ?> factory =
                context.getBean("kafkaListenerContainerFactory", ConcurrentKafkaListenerContainerFactory.class);
            assertThat(factory.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        });
    }

    @Test
    void groupIdDefaultsToApplicationName() {
        runner().run(context -> assertThat(context.getBean(ConsumerFactory.class).getConfigurationProperties())
            .containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "test-service"));
    }

    @SuppressWarnings("unchecked")
    private static KafkaTemplate<String, Object> template(org.springframework.context.ApplicationContext context) {
        return context.getBean("kafkaTemplate", KafkaTemplate.class);
    }

    // Records of earlier tests stay in the topic, so wait for the one this test sent
    private static TestEvent awaitEvent(Listener listener, UUID idempotencyKey) throws InterruptedException {
        TestEvent event;
        do {
            event = listener.received.poll(30, TimeUnit.SECONDS);
        } while (event != null && !idempotencyKey.equals(event.idempotencyKey()));
        assertThat(event).as("event %s", idempotencyKey).isNotNull();
        return event;
    }

    private static ConsumerRecord<String, String> awaitDeadLetter(DeadLetterListener listener, String marker)
            throws InterruptedException {
        ConsumerRecord<String, String> record;
        do {
            record = listener.received.poll(30, TimeUnit.SECONDS);
        } while (record != null && !record.value().contains(marker));
        assertThat(record).as("dead letter containing %s", marker).isNotNull();
        return record;
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
