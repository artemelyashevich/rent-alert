package com.rentalert.kafka.config;

import com.rentalert.common.service.LockService;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.TraceIdRecordInterceptor;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.kafka.migration.MigrationControl;
import com.rentalert.kafka.migration.MonolithSource;
import com.rentalert.kafka.migration.RedisMigrationControl;
import com.rentalert.redis.service.CacheService;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.JacksonJsonMessageConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@EnableKafka
public abstract class AbstractKafkaConsumerConfiguration implements DisposableBean {

    public static final String DLT_SUFFIX = ".DLT";

    private static final Duration DLT_RETENTION = Duration.ofDays(7);

    @Value("${app.kafka.topic.partitions:3}")
    private int topicPartitions;

    @Value("${app.kafka.topic.replicas:1}")
    private short topicReplicas;

    @Value("${app.kafka.topic.min-insync-replicas:1}")
    private int topicMinInsyncReplicas;

    private DefaultKafkaProducerFactory<String, String> deadLetterProducerFactory;

    @Bean
    public ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties,
        @Value("${spring.application.name}") String applicationName) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, applicationName);
        props.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }

    @Bean
    @ConditionalOnProperty(value = "app.kafka.inbox.enabled")
    public InboxGuard inboxGuard(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager,
        @Value("${app.kafka.inbox.retention:P7D}") Duration retention) {
        return new InboxGuard(jdbcTemplate, new TransactionTemplate(transactionManager), retention);
    }

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
        ConcurrentKafkaListenerContainerFactoryConfigurer configurer, KafkaProperties kafkaProperties,
        ConsumerFactory<String, String> consumerFactory, JsonMapper jsonMapper) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure((ConcurrentKafkaListenerContainerFactory) factory, (ConsumerFactory) consumerFactory);
        factory.setRecordMessageConverter(new JacksonJsonMessageConverter(jsonMapper));
        factory.setCommonErrorHandler(errorHandler(kafkaProperties));
        factory.setRecordInterceptor(new TraceIdRecordInterceptor());
        return factory;
    }

    @Bean
    @ConditionalOnProperty(value = "app.migration.enabled", havingValue = "true")
    public MonolithSource monolithSource(@Value("${app.migration.source.url}") String url,
        @Value("${app.migration.source.username}") String username,
        @Value("${app.migration.source.password}") String password) {
        DataSource dataSource = new DriverManagerDataSource(url, username, password) {
            @Override
            public Connection getConnection() throws SQLException {
                Connection connection = super.getConnection();
                connection.setReadOnly(true);
                return connection;
            }
        };
        return new MonolithSource(new JdbcTemplate(dataSource));
    }

    @Bean
    @ConditionalOnMissingBean(MigrationControl.class)
    public MigrationControl redisMigrationControl(@Lazy CacheService cacheService, @Lazy LockService lockService) {
        return new RedisMigrationControl(cacheService, lockService);
    }

    protected CommonErrorHandler errorHandler(KafkaProperties kafkaProperties) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer(kafkaProperties), backOff());
        errorHandler.addNotRetryableExceptions(NonRetryableEventException.class);
        return errorHandler;
    }

    protected BackOff backOff() {
        return new FixedBackOff(1000L, 3);
    }

    protected ConsumerRecordRecoverer recoverer(KafkaProperties kafkaProperties) {
        deadLetterProducerFactory = new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(), new StringSerializer(), new StringSerializer());
        return new DeadLetterPublishingRecoverer(new KafkaTemplate<>(deadLetterProducerFactory), (record, exception) -> {
            log.error("Sending Kafka record topic={}, partition={}, offset={} to {}{}: {}", record.topic(), record.partition(), record.offset(), record.topic(), DLT_SUFFIX, exception.getMessage());
            return new TopicPartition(record.topic() + DLT_SUFFIX, -1);
        });
    }

    protected KafkaAdmin.NewTopics topicWithDeadLetter(String name, Duration retention) {
        return new KafkaAdmin.NewTopics(newTopic(name, retention), newTopic(name + DLT_SUFFIX, DLT_RETENTION));
    }

    private NewTopic newTopic(String name, Duration retention) {
        return newTopic(name, topicPartitions, retention);
    }

    /**
     * A topic with an explicit partition count and no dead-letter topic, for low-volume events whose loss is
     * acceptable. Replication settings are the same as for the other topics.
     */
    protected NewTopic newTopic(String name, int partitions, Duration retention) {
        return TopicBuilder.name(name)
            .partitions(partitions)
            .replicas(topicReplicas)
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retention.toMillis()))
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(topicMinInsyncReplicas))
            .build();
    }

    @Override
    public void destroy() {
        if (deadLetterProducerFactory != null) {
            deadLetterProducerFactory.destroy();
        }
    }
}
