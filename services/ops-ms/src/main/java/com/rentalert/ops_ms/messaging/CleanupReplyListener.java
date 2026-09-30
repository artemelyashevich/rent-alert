package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.cleanup.CleanupReply;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.ops_ms.cleanup.CleanupSagaOrchestrator;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CleanupReplyListener {

    private final CleanupSagaOrchestrator orchestrator;

    @KafkaListener(topics = KafkaTopics.CLEANUP_REPLIES, groupId = "ops.cleanup-replies")
    public void onReply(CleanupReply reply) {
        if (reply.sagaId() == null) {
            throw new NonRetryableEventException("Cleanup reply without saga id");
        }
        orchestrator.onReply(reply, Instant.now());
    }
}
