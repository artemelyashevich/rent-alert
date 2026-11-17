package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.cleanup.CleanupReply;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.ops_ms.cleanup.CleanupSagaOrchestrator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CleanupReplyListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "ops.cleanup-replies";

    private final CleanupSagaOrchestrator orchestrator;

    public CleanupReplyListener(CleanupSagaOrchestrator orchestrator) {
        super(CONSUMER);
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = KafkaTopics.CLEANUP_REPLIES, groupId = CONSUMER)
    public void onReply(CleanupReply reply) {
        if (reply.sagaId() == null) {
            throw new NonRetryableEventException("Cleanup reply without saga id");
        }
        processOnce(replyKey(reply), () -> orchestrator.onReply(reply, Instant.now()));
    }

    private static UUID replyKey(CleanupReply reply) {
        return UUID.nameUUIDFromBytes(("cleanup-reply:" + reply.sagaId() + ":" + reply.step() + ":" + reply.service()).getBytes(StandardCharsets.UTF_8));
    }
}
