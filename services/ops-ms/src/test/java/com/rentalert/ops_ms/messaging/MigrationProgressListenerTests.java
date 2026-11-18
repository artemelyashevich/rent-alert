package com.rentalert.ops_ms.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.migration.MigrationProgress;
import com.rentalert.ops_ms.migration.MigrationSagaOrchestrator;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MigrationProgressListenerTests {

    private final MigrationSagaOrchestrator orchestrator = mock(MigrationSagaOrchestrator.class);
    private final MigrationProgressListener listener = new MigrationProgressListener(orchestrator);

    @Test
    void progressIsPassedToOrchestrator() {
        MigrationProgress progress = new MigrationProgress(UUID.randomUUID(), 1, "listing-ms", "listings", 10, 100);

        listener.onProgress(progress);

        verify(orchestrator).onProgress(any(), any());
    }
}
