package com.rentalert.ops_ms.migration;

import com.rentalert.common.service.LockService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MigrationSagaScheduler {

    private static final Duration LOCK_LEASE = Duration.ofMinutes(5);

    private final LockService lockService;
    private final MigrationSagaOrchestrator orchestrator;

    @Scheduled(fixedDelayString = "${app.migration.saga.timeout-check-interval:PT1M}")
    public void checkTimeouts() {
        lockService.tryWithLock("scheduled:migration-saga:timeouts", LOCK_LEASE, () -> orchestrator.checkTimeouts(Instant.now()));
    }
}
