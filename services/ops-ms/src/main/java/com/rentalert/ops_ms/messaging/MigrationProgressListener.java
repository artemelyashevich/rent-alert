package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.migration.MigrationProgress;
import com.rentalert.ops_ms.migration.MigrationSagaOrchestrator;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MigrationProgressListener {

    private static final String CONSUMER = "ops.migration-progress";

    private final MigrationSagaOrchestrator orchestrator;

    @KafkaListener(topics = KafkaTopics.MIGRATION_PROGRESS, groupId = CONSUMER)
    public void onProgress(MigrationProgress progress) {
        orchestrator.onProgress(progress, Instant.now());
    }
}
