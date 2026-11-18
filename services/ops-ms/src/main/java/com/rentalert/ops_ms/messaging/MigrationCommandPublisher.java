package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.migration.MigrationCommand;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MigrationCommandPublisher extends AbstractOutboxEventPublisher {

    public void publishCommand(MigrationCommand command) {
        publish(KafkaTopics.MIGRATION_COMMANDS, command.migrationId().toString(), command);
    }
}
