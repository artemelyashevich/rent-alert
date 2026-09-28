package com.rentalert.ops_ms.configuration;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.config.AbstractKafkaConsumerConfiguration;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaConsumerConfiguration extends AbstractKafkaConsumerConfiguration {

    @Bean
    public KafkaAdmin.NewTopics auditEventsTopics() {
        return topicWithDeadLetter(KafkaTopics.AUDIT_EVENTS, Duration.ofDays(7));
    }
}
