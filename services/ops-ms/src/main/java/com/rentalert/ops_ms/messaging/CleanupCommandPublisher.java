package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.cleanup.CleanupCommand;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class CleanupCommandPublisher extends AbstractOutboxEventPublisher {

    public void publishCommand(CleanupCommand command) {
        publish(KafkaTopics.CLEANUP_COMMANDS, command.service(), command);
    }
}
