package com.rentalert.roommate_ms.config;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.config.AbstractKafkaConsumerConfiguration;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaConsumerConfiguration extends AbstractKafkaConsumerConfiguration {

    @Bean
    public KafkaAdmin.NewTopics roommateComplaintsTopics() {
        return topicWithDeadLetter(KafkaTopics.ROOMMATE_COMPLAINTS, Duration.ofDays(7));
    }

    @Bean
    public KafkaAdmin.NewTopics listingsDeletedTopics() {
        return topicWithDeadLetter(KafkaTopics.LISTINGS_DELETED, Duration.ofDays(7));
    }
}
