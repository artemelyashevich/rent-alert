package com.rentalert.processing_ms.scheduler;

import com.rentalert.common.service.LockService;
import com.rentalert.processing_ms.messaging.OpsEventPublisher;
import com.rentalert.processing_ms.service.CurrencyRateService;
import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobScope;
import com.rentalert.dto.event.job.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class CurrencyRateScheduler {

    private static final Duration LOCK_LEASE = Duration.ofSeconds(600);

    private final CurrencyRateService currencyRateService;
    private final LockService lockService;
    private final OpsEventPublisher opsEventPublisher;

    @Scheduled(fixedDelayString = "PT6H", initialDelayString = "PT5M")
    public void job() {
        boolean ran = lockService.tryWithLock("scheduled:currency-rate-sync", LOCK_LEASE, this::syncAndReport);
        if (!ran) {
            log.debug("Currency rates sync skipped — another instance owns the lock");
        }
    }

    private void syncAndReport() {
        log.info("Starting scheduled currency rates sync");
        Instant started = Instant.now();
        JobExecutionEvent.JobExecutionEventBuilder report = JobExecutionEvent.builder()
            .jobName(JobName.CURRENCY_RATE_SYNC)
            .initiator(JobInitiator.INTERNAL)
            .executionScope(JobScope.CACHE_DB_SYNC)
            .description("Fetch new currency rates & sync cache")
            .affectedRecords(0L)
            .startedAt(started);
        try {
            long affected = currencyRateService.syncCurrencyRates();
            report.affectedRecords(affected).status(JobStatus.SUCCESS);
            log.info("Currency rates sync completed successfully");
        } catch (Exception e) {
            report.status(JobStatus.FAILED).errorMessage(e.getMessage());
            log.error("Currency rates sync failed: {}", e.getMessage(), e);
        }
        Instant finished = Instant.now();
        report.finishedAt(finished).executionTimeMs(Duration.between(started, finished).toMillis());
        try {
            opsEventPublisher.publishJobExecution(report.build());
        } catch (Exception e) {
            log.error("Failed to report currency rates sync to ops", e);
        }
    }
}
