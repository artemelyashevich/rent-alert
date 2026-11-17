package com.rentalert.realtime_ms.config;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.config.AbstractKafkaConsumerConfiguration;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaConsumerConfiguration extends AbstractKafkaConsumerConfiguration {

    @Bean
    public KafkaAdmin.NewTopics realtimePushTopics() {
        return topicWithDeadLetter(KafkaTopics.REALTIME_PUSH, Duration.ofDays(1));
    }
}
