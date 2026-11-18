package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.migration.MigrationReply;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.ops_ms.migration.MigrationSagaOrchestrator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MigrationReplyListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "ops.migration-replies";

    private final MigrationSagaOrchestrator orchestrator;

    public MigrationReplyListener(MigrationSagaOrchestrator orchestrator) {
        super(CONSUMER);
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = KafkaTopics.MIGRATION_REPLIES, groupId = CONSUMER)
    public void onReply(MigrationReply reply) {
        if (reply.migrationId() == null) {
            throw new NonRetryableEventException("Migration reply without migration id");
        }
        processOnce(replyKey(reply), () -> orchestrator.onReply(reply, Instant.now()));
    }

    private static UUID replyKey(MigrationReply reply) {
        return UUID.nameUUIDFromBytes(("migration-reply:" + reply.migrationId() + ":" + reply.step() + ":" + reply.service()).getBytes(StandardCharsets.UTF_8));
    }
}
