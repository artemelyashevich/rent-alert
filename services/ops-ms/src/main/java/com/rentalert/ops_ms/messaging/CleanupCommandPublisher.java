package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.cleanup.CleanupCommand;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Sends cleanup saga commands to participant services through the transactional outbox. Must be called inside the saga
 * transaction, so a command is sent only if the saga state is committed.
 */
@Component
public class CleanupCommandPublisher extends AbstractOutboxEventPublisher {

    /**
     * Stores the command in the outbox of the current transaction, keyed by the target service.
     */
    public void publishCommand(CleanupCommand command) {
        publish(KafkaTopics.CLEANUP_COMMANDS, command.service(), command);
    }
}
