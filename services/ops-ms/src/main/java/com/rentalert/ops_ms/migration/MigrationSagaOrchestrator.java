package com.rentalert.ops_ms.migration;

import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.AppNotFoundException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.event.migration.MigrationCommand;
import com.rentalert.dto.event.migration.MigrationProgress;
import com.rentalert.dto.event.migration.MigrationReply;
import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.MigrationStartRequest;
import com.rentalert.dto.migration.MigrationStatus;
import com.rentalert.dto.migration.MigrationStepStatus;
import com.rentalert.ops_ms.domain.MigrationRun;
import com.rentalert.ops_ms.domain.MigrationRunStep;
import com.rentalert.ops_ms.messaging.MigrationCommandPublisher;
import com.rentalert.ops_ms.repository.MigrationRunRepository;
import com.rentalert.ops_ms.repository.MigrationRunStepRepository;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class MigrationSagaOrchestrator {

    private static final List<String> SERVICE_ORDER = List.of("identity-ms", "listing-ms", "processing-ms", "notification-ms", "roommate-ms", "ops-ms");
    private static final List<MigrationStepStatus> IN_FLIGHT = List.of(MigrationStepStatus.SENT, MigrationStepStatus.RUNNING);
    private static final Duration CANCEL_FLAG_TTL = Duration.ofDays(1);

    private final CacheService cacheService;
    private final MigrationCommandPublisher migrationCommandPublisher;
    private final MigrationRunRepository runRepository;
    private final MigrationRunStepRepository stepRepository;
    private final TransactionTemplate transactionTemplate;
    private final Duration stepTimeout;
    private final int maxAttempts;

    public MigrationSagaOrchestrator(CacheService cacheService, MigrationCommandPublisher migrationCommandPublisher, MigrationRunRepository runRepository,
                                     MigrationRunStepRepository stepRepository, TransactionTemplate transactionTemplate,
                                     @Value("${app.migration.saga.step-timeout:PT2H}") Duration stepTimeout, @Value("${app.migration.saga.max-attempts:2}") int maxAttempts) {
        this.cacheService = cacheService;
        this.migrationCommandPublisher = migrationCommandPublisher;
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.transactionTemplate = transactionTemplate;
        this.stepTimeout = stepTimeout;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public UUID start(MigrationStartRequest request, Long requestedBy) {
        List<String> services = selectServices(request.services());
        MigrationSettings settings = normalize(request.settings());
        if (runRepository.existsByStatus(MigrationStatus.RUNNING)) {
            throw new AppException(InternalExceptionCode.EX409_JOB_ALREADY_RUNNING, "another migration is already running", HttpStatus.CONFLICT);
        }
        Instant now = Instant.now();
        MigrationRun run = runRepository.save(MigrationRun.start(UUID.randomUUID(), settings, requestedBy, now));
        List<MigrationRunStep> steps = stepRepository.saveAll(IntStream.range(0, services.size())
            .mapToObj(i -> MigrationRunStep.pending(run.getId(), i + 1, services.get(i), now))
            .toList());
        log.info("[MIGRATION] run {} started with steps {}", run.getId(), services);
        send(run, steps.getFirst(), now);
        return run.getId();
    }

    @Transactional
    public void onReply(MigrationReply reply, Instant now) {
        MigrationRunStep step = stepRepository.findByMigrationIdAndStepOrderForUpdate(reply.migrationId(), reply.step())
            .filter(s -> isInFlight(s) && s.getService().equals(reply.service()))
            .orElse(null);
        if (step == null) {
            log.debug("[MIGRATION] ignoring reply for run {} step {}: unknown or already handled", reply.migrationId(), reply.step());
            return;
        }
        MigrationRun run = runRepository.findById(reply.migrationId()).orElseThrow();
        step.setTables(reply.tables());
        step.setUpdatedAt(now);
        if (reply.success()) {
            step.setStatus(MigrationStepStatus.COMPLETED);
            step.setFinishedAt(now);
            log.info("[MIGRATION] run {} step {} ({}) completed", reply.migrationId(), reply.step(), reply.service());
            proceed(run, now);
        } else {
            log.error("[MIGRATION] run {} step {} ({}) failed: {}", reply.migrationId(), reply.step(), reply.service(), reply.error());
            fail(run, step, MigrationStepStatus.FAILED, Objects.requireNonNullElse(reply.error(), "migration failed in " + reply.service()), now);
        }
    }

    @Transactional
    public void onProgress(MigrationProgress progress, Instant now) {
        int updated = stepRepository.updateProgress(progress.migrationId(), progress.step(), progress.service(), progress.processed(), progress.total(), now, IN_FLIGHT);
        if (updated > 0) {
            stepRepository.markRunningIfSent(progress.migrationId(), progress.step());
        }
    }

    @Transactional
    public void cancel(UUID migrationId) {
        MigrationRun run = runRepository.findById(migrationId)
            .orElseThrow(() -> new AppNotFoundException("migration run not found: " + migrationId));
        if (run.getStatus() != MigrationStatus.RUNNING) {
            throw new AppException(InternalExceptionCode.EX409, "migration run is already " + run.getStatus(), HttpStatus.CONFLICT);
        }
        Instant now = Instant.now();
        run.setStatus(MigrationStatus.CANCELLED);
        run.setFinishedAt(now);
        for (MigrationRunStep step : stepRepository.findAllByMigrationIdOrderByStepOrder(migrationId)) {
            if (step.getStatus() == MigrationStepStatus.PENDING || isInFlight(step)) {
                step.setStatus(MigrationStepStatus.CANCELLED);
                step.setFinishedAt(now);
                step.setUpdatedAt(now);
            }
        }
        cacheService.put(CacheType.MIGRATION_CANCELLED, migrationId, Boolean.TRUE, CANCEL_FLAG_TTL);
        log.info("[MIGRATION] run {} cancelled", migrationId);
    }

    public void checkTimeouts(Instant now) {
        for (MigrationRunStep candidate : stepRepository.findAllByStatusInAndUpdatedAtBefore(IN_FLIGHT, now.minus(stepTimeout))) {
            transactionTemplate.executeWithoutResult(status -> checkTimeout(candidate.getMigrationId(), candidate.getStepOrder(), now));
        }
    }

    private void checkTimeout(UUID migrationId, int stepOrder, Instant now) {
        Instant idleSince = now.minus(stepTimeout);
        MigrationRunStep step = stepRepository.findByMigrationIdAndStepOrderForUpdate(migrationId, stepOrder)
            .filter(s -> isInFlight(s) && s.getUpdatedAt().isBefore(idleSince))
            .orElse(null);
        if (step == null) {
            log.debug("[MIGRATION] run {} step {}: timeout check skipped, already handled or not expired", migrationId, stepOrder);
            return;
        }
        MigrationRun run = runRepository.findById(migrationId).orElseThrow();
        if (step.getAttempts() < maxAttempts) {
            log.warn("[MIGRATION] run {} step {} ({}) got no progress, sending again", migrationId, stepOrder, step.getService());
            send(run, step, now);
            return;
        }
        log.error("[MIGRATION] run {} step {} ({}) timed out after {} attempts", migrationId, stepOrder, step.getService(), step.getAttempts());
        fail(run, step, MigrationStepStatus.TIMED_OUT, "no reply or progress from %s after %d attempts".formatted(step.getService(), step.getAttempts()), now);
    }

    private void proceed(MigrationRun run, Instant now) {
        List<MigrationRunStep> steps = stepRepository.findAllByMigrationIdOrderByStepOrder(run.getId());
        steps.stream()
            .filter(s -> s.getStatus() == MigrationStepStatus.PENDING)
            .findFirst()
            .ifPresentOrElse(next -> send(run, next, now), () -> complete(run, now));
    }

    private void send(MigrationRun run, MigrationRunStep step, Instant now) {
        step.setStatus(MigrationStepStatus.SENT);
        step.setAttempts(step.getAttempts() + 1);
        step.setSentAt(now);
        if (step.getStartedAt() == null) {
            step.setStartedAt(now);
        }
        step.setUpdatedAt(now);
        migrationCommandPublisher.publishCommand(new MigrationCommand(run.getId(), step.getStepOrder(), step.getService(), run.getSettings()));
        log.info("migration {} step {} service={} -> {}", run.getId(), step.getStepOrder(), step.getService(), step.getStatus());
    }

    private void complete(MigrationRun run, Instant now) {
        run.setStatus(MigrationStatus.COMPLETED);
        run.setFinishedAt(now);
        log.info("[MIGRATION] run {} completed", run.getId());
    }

    private void fail(MigrationRun run, MigrationRunStep failed, MigrationStepStatus failedStatus, String error, Instant now) {
        failed.setStatus(failedStatus);
        failed.setError(error);
        failed.setFinishedAt(now);
        failed.setUpdatedAt(now);
        for (MigrationRunStep step : stepRepository.findAllByMigrationIdOrderByStepOrder(run.getId())) {
            if (step.getStatus() == MigrationStepStatus.PENDING) {
                step.setStatus(MigrationStepStatus.SKIPPED);
                step.setUpdatedAt(now);
            }
        }
        run.setStatus(MigrationStatus.FAILED);
        run.setFinishedAt(now);
        log.error("[MIGRATION] run {} failed at step {} ({})", run.getId(), failed.getStepOrder(), failed.getService());
    }

    private static boolean isInFlight(MigrationRunStep step) {
        return IN_FLIGHT.contains(step.getStatus());
    }

    private static List<String> selectServices(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return SERVICE_ORDER;
        }
        List<String> unknown = requested.stream().filter(service -> service == null || !SERVICE_ORDER.contains(service)).toList();
        if (!unknown.isEmpty()) {
            throw new AppBadRequestException("unknown migration services: " + unknown);
        }
        return SERVICE_ORDER.stream().filter(requested::contains).toList();
    }

    private static MigrationSettings normalize(MigrationSettings settings) {
        if (settings == null) {
            return new MigrationSettings(MigrationSettings.DEFAULT_BATCH_SIZE, null, null, false, List.of());
        }
        int batchSize = settings.batchSize() == 0 ? MigrationSettings.DEFAULT_BATCH_SIZE : settings.batchSize();
        if (batchSize < 1 || batchSize > MigrationSettings.MAX_BATCH_SIZE) {
            throw new AppBadRequestException("batchSize must be between 1 and " + MigrationSettings.MAX_BATCH_SIZE);
        }
        if (settings.limitPerTable() != null && settings.limitPerTable() <= 0) {
            throw new AppBadRequestException("limitPerTable must be positive");
        }
        List<String> tables = settings.tables() == null ? List.of() : settings.tables();
        return new MigrationSettings(batchSize, settings.limitPerTable(), settings.since(), settings.dryRun(), tables);
    }
}
