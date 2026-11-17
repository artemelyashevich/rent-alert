package com.rentalert.kafka.config;

import com.rentalert.kafka.audit.CurrentUserIdProvider;
import com.rentalert.kafka.audit.UserAuditAspect;
import com.rentalert.kafka.lifecycle.ServiceStartedPublisher;
import com.rentalert.kafka.outbox.OutboxPublisher;
import com.rentalert.kafka.outbox.OutboxRelay;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@EnableScheduling
public abstract class AbstractKafkaProducerConfiguration {

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties, JsonMapper jsonMapper) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        props.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new JacksonJsonSerializer<>(jsonMapper).noTypeInfo());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ServiceStartedPublisher serviceStartedPublisher(KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${spring.application.name}") String applicationName) {
        return new ServiceStartedPublisher(kafkaTemplate, applicationName);
    }

    @Bean
    @ConditionalOnProperty(value = "app.kafka.outbox.enabled", matchIfMissing = true)
    public OutboxPublisher outboxPublisher(JdbcTemplate jdbcTemplate, JsonMapper jsonMapper) {
        return new OutboxPublisher(jdbcTemplate, jsonMapper);
    }

    @Bean
    @ConditionalOnProperty(value = "app.kafka.outbox.enabled", matchIfMissing = true)
    public OutboxRelay outboxRelay(
        JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager,
        KafkaTemplate<String, Object> kafkaTemplate, JsonMapper jsonMapper,
        @Value("${app.kafka.outbox.batch-size:100}") int batchSize,
        @Value("${app.kafka.outbox.send-timeout:PT10S}") Duration sendTimeout,
        @Value("${app.kafka.outbox.retention:P7D}") Duration retention
    ) {
        return new OutboxRelay(jdbcTemplate, new TransactionTemplate(transactionManager), kafkaTemplate, jsonMapper, batchSize, sendTimeout, retention);
    }

    @Bean
    @ConditionalOnProperty(value = "app.kafka.user-audit.enabled", havingValue = "true")
    public UserAuditAspect userAuditAspect(ObjectProvider<CurrentUserIdProvider> currentUserIdProvider,
        OutboxPublisher outboxPublisher, PlatformTransactionManager transactionManager) {
        return new UserAuditAspect(currentUserIdProvider.getIfAvailable(), outboxPublisher, transactionManager);
    }
}
