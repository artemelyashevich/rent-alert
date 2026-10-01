package com.rentalert.notification_ms.config;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.config.AbstractKafkaConsumerConfiguration;
import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaConsumerConfiguration extends AbstractKafkaConsumerConfiguration {

    @Bean
    public KafkaAdmin.NewTopics notificationRequestsTopics() {
        return topicWithDeadLetter(KafkaTopics.NOTIFICATION_REQUESTS, Duration.ofDays(3));
    }

    @Bean
    public NewTopic serviceStartedTopic() {
        return newTopic(KafkaTopics.SERVICE_STARTED, 1, Duration.ofDays(1));
    }

    @Bean
    public KafkaAdmin.NewTopics priorityNotificationRequestsTopics() {
        return topicWithDeadLetter(KafkaTopics.NOTIFICATION_REQUESTS_PRIORITY, Duration.ofDays(1));
    }
}
