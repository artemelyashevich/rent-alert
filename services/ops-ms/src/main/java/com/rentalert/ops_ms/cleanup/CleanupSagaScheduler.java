package com.rentalert.ops_ms.cleanup;

import com.rentalert.common.service.LockService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupSagaScheduler {

    private static final Duration LOCK_LEASE = Duration.ofMinutes(5);

    private final CleanupSagaOrchestrator orchestrator;
    private final LockService lockService;

    @Scheduled(cron = "${app.cleanup.cron:0 0 0 * * *}", zone = "${app.bot.timezone:Europe/Minsk}")
    public void startCleanup() {
        if (!lockService.tryWithLock("scheduled:cleanup-saga:start", LOCK_LEASE, () -> orchestrator.start(Instant.now()))) {
            log.debug("[CLEANUP] saga start skipped — another instance owns the lock");
        }
    }

    @Scheduled(fixedDelayString = "${app.cleanup.saga.timeout-check-interval:PT1M}")
    public void checkTimeouts() {
        lockService.tryWithLock("scheduled:cleanup-saga:timeouts", LOCK_LEASE, () -> orchestrator.checkTimeouts(Instant.now()));
    }
}
