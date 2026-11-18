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

    @Bean
    public KafkaAdmin.NewTopics userEventsTopics() {
        return topicWithDeadLetter(KafkaTopics.USER_EVENTS, Duration.ofDays(7));
    }

    @Bean
    public KafkaAdmin.NewTopics jobEventsTopics() {
        return topicWithDeadLetter(KafkaTopics.JOB_EVENTS, Duration.ofDays(7));
    }

    @Bean
    public KafkaAdmin.NewTopics cleanupCommandsTopics() {
        return topicWithDeadLetter(KafkaTopics.CLEANUP_COMMANDS, Duration.ofDays(7));
    }

    @Bean
    public KafkaAdmin.NewTopics cleanupRepliesTopics() {
        return topicWithDeadLetter(KafkaTopics.CLEANUP_REPLIES, Duration.ofDays(7));
    }

    @Bean
    public KafkaAdmin.NewTopics migrationCommandsTopics() {
        return topicWithDeadLetter(KafkaTopics.MIGRATION_COMMANDS, Duration.ofDays(7));
    }

    @Bean
    public KafkaAdmin.NewTopics migrationProgressTopics() {
        return topicWithDeadLetter(KafkaTopics.MIGRATION_PROGRESS, Duration.ofDays(7));
    }

    @Bean
    public KafkaAdmin.NewTopics migrationRepliesTopics() {
        return topicWithDeadLetter(KafkaTopics.MIGRATION_REPLIES, Duration.ofDays(7));
    }
}
