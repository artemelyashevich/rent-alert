package com.rentalert.ops_ms.cleanup;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.cleanup.CleanupCommand;
import com.rentalert.dto.event.cleanup.CleanupReply;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobScope;
import com.rentalert.dto.event.job.JobStatus;
import com.rentalert.ops_ms.domain.CleanupSaga;
import com.rentalert.ops_ms.domain.CleanupSagaStatus;
import com.rentalert.ops_ms.domain.CleanupSagaStep;
import com.rentalert.ops_ms.domain.CleanupStepStatus;
import com.rentalert.ops_ms.messaging.CleanupCommandPublisher;
import com.rentalert.ops_ms.repository.CleanupSagaRepository;
import com.rentalert.ops_ms.repository.CleanupSagaStepRepository;
import com.rentalert.ops_ms.service.JobSettingService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class CleanupSagaOrchestrator {

    private final CleanupSagaRepository sagaRepository;
    private final CleanupSagaStepRepository stepRepository;
    private final CleanupCommandPublisher cleanupCommandPublisher;
    private final JobSettingService jobSettingService;
    private final List<String> steps;
    private final Duration stepTimeout;
    private final int maxAttempts;

    public CleanupSagaOrchestrator(CleanupSagaRepository sagaRepository, CleanupSagaStepRepository stepRepository, CleanupCommandPublisher cleanupCommandPublisher, JobSettingService jobSettingService,
                                   @Value("${app.cleanup.saga.steps:listing-ms,processing-ms,notification-ms,roommate-ms,ops-ms}") List<String> steps,
                                   @Value("${app.cleanup.saga.step-timeout:PT30M}") Duration stepTimeout, @Value("${app.cleanup.saga.max-attempts:3}") int maxAttempts) {
        this.sagaRepository = sagaRepository;
        this.stepRepository = stepRepository;
        this.cleanupCommandPublisher = cleanupCommandPublisher;
        this.jobSettingService = jobSettingService;
        this.steps = steps;
        this.stepTimeout = stepTimeout;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public Optional<UUID> start(Instant now) {
        return start(now, JobInitiator.INTERNAL);
    }

    @Transactional
    public Optional<UUID> start(Instant now, JobInitiator initiator) {
        if (sagaRepository.existsByStatus(CleanupSagaStatus.RUNNING)) {
            log.warn("[CLEANUP] previous cleanup saga is still running, skipping this run");
            return Optional.empty();
        }
        CleanupSaga saga = sagaRepository.save(CleanupSaga.start(now, initiator));
        List<CleanupSagaStep> sagaSteps = stepRepository.saveAll(IntStream.range(0, steps.size())
            .mapToObj(i -> CleanupSagaStep.pending(saga.getSagaId(), i + 1, steps.get(i)))
            .toList());
        log.info("[CLEANUP] saga {} started with steps {}", saga.getSagaId(), steps);
        send(saga, sagaSteps.getFirst(), now);
        return Optional.of(saga.getSagaId());
    }

    @Transactional
    public void onReply(CleanupReply reply, Instant now) {
        Optional<CleanupSaga> saga = sagaRepository.findBySagaId(reply.sagaId());
        Optional<CleanupSagaStep> step = stepRepository.findBySagaIdAndStepOrder(reply.sagaId(), reply.step());
        if (saga.isEmpty() || step.isEmpty() || step.get().getStatus() != CleanupStepStatus.SENT) {
            log.debug("[CLEANUP] ignoring reply for saga {} step {}: unknown or already handled", reply.sagaId(), reply.step());
            return;
        }
        CleanupSagaStep done = step.get();
        done.setDeleted(reply.deleted());
        done.setFailed(reply.failed());
        done.setDurationMs(reply.durationMs());
        done.setErrors(reply.errors().isEmpty() ? null : String.join("; ", reply.errors()));
        done.setStatus(reply.failed() > 0 || !reply.errors().isEmpty() ? CleanupStepStatus.DONE_WITH_ERRORS : CleanupStepStatus.DONE);
        done.setFinishedAt(now);
        log.info("[CLEANUP] saga {} step {} ({}) done: deleted={} failed={} time={}ms", reply.sagaId(), reply.step(), done.getService(), reply.deleted(), reply.failed(), reply.durationMs());
        proceed(saga.get(), now);
    }

    @Transactional
    public void checkTimeouts(Instant now) {
        for (CleanupSagaStep step : stepRepository.findAllByStatusAndSentAtBefore(CleanupStepStatus.SENT, now.minus(stepTimeout))) {
            CleanupSaga saga = sagaRepository.findBySagaId(step.getSagaId()).orElseThrow();
            if (step.getAttempts() < maxAttempts) {
                log.warn("[CLEANUP] saga {} step {} ({}) got no reply, sending again", step.getSagaId(), step.getStepOrder(), step.getService());
                send(saga, step, now);
                continue;
            }
            log.error("[CLEANUP] saga {} step {} ({}) timed out after {} attempts", step.getSagaId(), step.getStepOrder(), step.getService(), step.getAttempts());
            step.setStatus(CleanupStepStatus.TIMED_OUT);
            step.setFinishedAt(now);
            step.setErrors("no reply from %s after %d attempts".formatted(step.getService(), step.getAttempts()));
            proceed(saga, now);
        }
    }

    private void proceed(CleanupSaga saga, Instant now) {
        List<CleanupSagaStep> sagaSteps = stepRepository.findAllBySagaIdOrderByStepOrder(saga.getSagaId());
        Optional<CleanupSagaStep> next = sagaSteps.stream().filter(s -> s.getStatus() == CleanupStepStatus.PENDING).findFirst();
        if (next.isPresent()) {
            send(saga, next.get(), now);
            return;
        }
        finish(saga, sagaSteps, now);
    }

    private void send(CleanupSaga saga, CleanupSagaStep step, Instant now) {
        step.setStatus(CleanupStepStatus.SENT);
        step.setAttempts(step.getAttempts() + 1);
        step.setSentAt(now);
        stepRepository.save(step);
        cleanupCommandPublisher.publishCommand(new CleanupCommand(saga.getSagaId(), step.getStepOrder(), step.getService(), saga.getReferenceTime()));
    }

    private void finish(CleanupSaga saga, List<CleanupSagaStep> sagaSteps, Instant now) {
        long deleted = sagaSteps.stream().mapToLong(CleanupSagaStep::getDeleted).sum();
        long failed = sagaSteps.stream().mapToLong(CleanupSagaStep::getFailed).sum();
        boolean clean = sagaSteps.stream().allMatch(s -> s.getStatus() == CleanupStepStatus.DONE);
        saga.setDeleted(deleted);
        saga.setFailed(failed);
        saga.setFinishedAt(now);
        saga.setStatus(clean ? CleanupSagaStatus.COMPLETED : CleanupSagaStatus.COMPLETED_WITH_ERRORS);
        sagaRepository.save(saga);

        long durationMs = Duration.between(saga.getReferenceTime(), now).toMillis();
        jobSettingService.record(JobExecutionEvent.builder()
            .jobName(JobName.CLEANUP)
            .initiator(saga.getInitiator())
            .executionScope(JobScope.DB)
            .description("Cleanup saga " + saga.getSagaId())
            .affectedRecords(deleted)
            .startedAt(saga.getReferenceTime())
            .finishedAt(now)
            .executionTimeMs(durationMs)
            .status(clean ? JobStatus.SUCCESS : JobStatus.FAILED)
            .errorMessage(clean ? null : failureSummary(failed, sagaSteps))
            .build());
        log.info("[CLEANUP] saga {} finished: status={} deleted={} failed={} time={}ms", saga.getSagaId(), saga.getStatus(), deleted, failed, durationMs);
    }

    private static String failureSummary(long failed, List<CleanupSagaStep> sagaSteps) {
        String stepErrors = sagaSteps.stream()
            .filter(s -> StringUtils.hasText(s.getErrors()))
            .map(s -> s.getService() + ": " + s.getErrors())
            .collect(Collectors.joining("; "));
        return "failed to delete " + failed + " rows" + (stepErrors.isEmpty() ? "" : "; " + stepErrors);
    }
}
