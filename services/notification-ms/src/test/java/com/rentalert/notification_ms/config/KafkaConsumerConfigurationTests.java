package com.rentalert.notification_ms.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.dto.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

class KafkaConsumerConfigurationTests {

    private final KafkaConsumerConfiguration configuration = new KafkaConsumerConfiguration();

    @Test
    void serviceStartedTopicIsPartitionedForScaling() {
        NewTopic topic = configuration.serviceStartedTopic();

        assertThat(topic.name()).isEqualTo(KafkaTopics.SERVICE_STARTED);
        assertThat(topic.numPartitions()).isEqualTo(3);
    }
}
