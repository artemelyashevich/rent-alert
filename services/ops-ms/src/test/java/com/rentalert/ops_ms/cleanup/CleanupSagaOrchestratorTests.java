package com.rentalert.ops_ms.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.cleanup.CleanupCommand;
import com.rentalert.dto.event.cleanup.CleanupReply;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobStatus;
import com.rentalert.kafka.outbox.OutboxPublisher;
import com.rentalert.ops_ms.domain.CleanupSaga;
import com.rentalert.ops_ms.domain.CleanupSagaStatus;
import com.rentalert.ops_ms.domain.CleanupSagaStep;
import com.rentalert.ops_ms.domain.CleanupStepStatus;
import com.rentalert.ops_ms.repository.CleanupSagaRepository;
import com.rentalert.ops_ms.repository.CleanupSagaStepRepository;
import com.rentalert.ops_ms.service.JobSettingService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CleanupSagaOrchestratorTests {

    private static final Instant MIDNIGHT = Instant.parse("2026-09-30T21:00:00Z");

    private final CleanupSagaRepository sagaRepository = mock(CleanupSagaRepository.class);
    private final CleanupSagaStepRepository stepRepository = mock(CleanupSagaStepRepository.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final JobSettingService jobSettingService = mock(JobSettingService.class);
    private final CleanupSagaOrchestrator orchestrator = new CleanupSagaOrchestrator(sagaRepository, stepRepository, outboxPublisher, jobSettingService,
        List.of("listing-ms", "ops-ms"), Duration.ofMinutes(30), 3);

    private final List<CleanupSaga> sagas = new ArrayList<>();
    private final List<CleanupSagaStep> steps = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(sagaRepository.save(any())).thenAnswer(invocation -> {
            CleanupSaga saga = invocation.getArgument(0);
            if (!sagas.contains(saga)) {
                sagas.add(saga);
            }
            return saga;
        });
        when(sagaRepository.existsByStatus(any())).thenAnswer(invocation -> sagas.stream().anyMatch(s -> s.getStatus() == invocation.getArgument(0)));
        when(sagaRepository.findBySagaId(any())).thenAnswer(invocation -> sagas.stream().filter(s -> s.getSagaId().equals(invocation.getArgument(0))).findFirst());
        when(stepRepository.saveAll(any())).thenAnswer(invocation -> {
            List<CleanupSagaStep> saved = new ArrayList<>((List<CleanupSagaStep>) invocation.getArgument(0));
            steps.addAll(saved);
            return saved;
        });
        when(stepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.findAllBySagaIdOrderByStepOrder(any())).thenAnswer(invocation -> steps.stream()
            .filter(s -> s.getSagaId().equals(invocation.getArgument(0))).sorted(Comparator.comparingInt(CleanupSagaStep::getStepOrder)).toList());
        when(stepRepository.findBySagaIdAndStepOrder(any(), anyInt())).thenAnswer(invocation -> steps.stream()
            .filter(s -> s.getSagaId().equals(invocation.getArgument(0)) && s.getStepOrder() == (int) invocation.getArgument(1)).findFirst());
        when(stepRepository.findAllByStatusAndSentAtBefore(any(), any())).thenAnswer(invocation -> steps.stream()
            .filter(s -> s.getStatus() == invocation.getArgument(0) && s.getSentAt().isBefore(invocation.getArgument(1))).toList());
    }

    @Test
    void runsStepsInOrderAndRecordsTheJob() {
        UUID sagaId = orchestrator.start(MIDNIGHT).orElseThrow();
        assertThat(sentCommands()).extracting(CleanupCommand::service).containsExactly("listing-ms");

        orchestrator.onReply(new CleanupReply(sagaId, 1, "listing-ms", 120, 0, 900, List.of()), MIDNIGHT.plusSeconds(1));
        assertThat(sentCommands()).extracting(CleanupCommand::service).containsExactly("listing-ms", "ops-ms");
        assertThat(sentCommands()).allMatch(command -> command.referenceTime().equals(MIDNIGHT));

        orchestrator.onReply(new CleanupReply(sagaId, 2, "ops-ms", 30, 0, 100, List.of()), MIDNIGHT.plusSeconds(2));

        CleanupSaga saga = sagas.getFirst();
        assertThat(saga.getStatus()).isEqualTo(CleanupSagaStatus.COMPLETED);
        assertThat(saga.getDeleted()).isEqualTo(150);
        ArgumentCaptor<JobExecutionEvent> job = ArgumentCaptor.forClass(JobExecutionEvent.class);
        verify(jobSettingService).record(job.capture());
        assertThat(job.getValue().getJobName()).isEqualTo(JobName.CLEANUP);
        assertThat(job.getValue().getAffectedRecords()).isEqualTo(150);
        assertThat(job.getValue().getExecutionTimeMs()).isEqualTo(2000);
        assertThat(job.getValue().getStatus()).isEqualTo(JobStatus.SUCCESS);
    }

    @Test
    void reportsRowsThatCouldNotBeDeleted() {
        UUID sagaId = orchestrator.start(MIDNIGHT).orElseThrow();
        orchestrator.onReply(new CleanupReply(sagaId, 1, "listing-ms", 100, 500, 900, List.of()), MIDNIGHT.plusSeconds(1));
        orchestrator.onReply(new CleanupReply(sagaId, 2, "ops-ms", 0, 0, 5, List.of("audit-log: connection refused")), MIDNIGHT.plusSeconds(2));

        assertThat(sagas.getFirst().getStatus()).isEqualTo(CleanupSagaStatus.COMPLETED_WITH_ERRORS);
        assertThat(sagas.getFirst().getFailed()).isEqualTo(500);
        ArgumentCaptor<JobExecutionEvent> job = ArgumentCaptor.forClass(JobExecutionEvent.class);
        verify(jobSettingService).record(job.capture());
        assertThat(job.getValue().getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getValue().getErrorMessage()).contains("failed to delete 500 rows").contains("ops-ms: audit-log: connection refused");
    }

    @Test
    void ignoresDuplicateReply() {
        UUID sagaId = orchestrator.start(MIDNIGHT).orElseThrow();
        CleanupReply reply = new CleanupReply(sagaId, 1, "listing-ms", 120, 0, 900, List.of());

        orchestrator.onReply(reply, MIDNIGHT.plusSeconds(1));
        orchestrator.onReply(reply, MIDNIGHT.plusSeconds(2));

        assertThat(sentCommands()).hasSize(2);
        assertThat(steps.getFirst().getDeleted()).isEqualTo(120);
    }

    @Test
    void resendsSilentStepAndGivesUpAfterMaxAttempts() {
        orchestrator.start(MIDNIGHT).orElseThrow();

        orchestrator.checkTimeouts(MIDNIGHT.plus(Duration.ofMinutes(31)));
        orchestrator.checkTimeouts(MIDNIGHT.plus(Duration.ofMinutes(62)));
        assertThat(steps.getFirst().getAttempts()).isEqualTo(3);

        orchestrator.checkTimeouts(MIDNIGHT.plus(Duration.ofMinutes(93)));

        assertThat(steps.getFirst().getStatus()).isEqualTo(CleanupStepStatus.TIMED_OUT);
        assertThat(sentCommands()).extracting(CleanupCommand::service).containsExactly("listing-ms", "listing-ms", "listing-ms", "ops-ms");
    }

    @Test
    void doesNotStartWhilePreviousSagaRuns() {
        orchestrator.start(MIDNIGHT).orElseThrow();

        Optional<UUID> second = orchestrator.start(MIDNIGHT.plus(Duration.ofDays(1)));

        assertThat(second).isEmpty();
        verify(outboxPublisher, times(1)).publish(eq(KafkaTopics.CLEANUP_COMMANDS), any(), any());
        verify(jobSettingService, never()).record(any());
    }

    private List<CleanupCommand> sentCommands() {
        ArgumentCaptor<Object> commands = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher, atLeast(0)).publish(eq(KafkaTopics.CLEANUP_COMMANDS), any(), commands.capture());
        return commands.getAllValues().stream().map(CleanupCommand.class::cast).toList();
    }
}
