package com.rentalert.ops_ms.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.rentalert.dto.migration.TableResult;
import com.rentalert.ops_ms.domain.MigrationRun;
import com.rentalert.ops_ms.domain.MigrationRunStep;
import com.rentalert.ops_ms.messaging.MigrationCommandPublisher;
import com.rentalert.ops_ms.repository.MigrationRunRepository;
import com.rentalert.ops_ms.repository.MigrationRunStepRepository;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class MigrationSagaOrchestratorTests {

    private static final MigrationSettings DEFAULTS = new MigrationSettings(MigrationSettings.DEFAULT_BATCH_SIZE, null, null, false, List.of());
    private static final List<TableResult> TABLES = List.of(new TableResult("listings", 10, 9, 0, 1));
    private static final List<String> ALL_SERVICES = List.of("identity-ms", "listing-ms", "processing-ms", "notification-ms", "roommate-ms", "ops-ms");

    private final Instant t0 = Instant.now();

    private final MigrationRunRepository runRepository = mock(MigrationRunRepository.class);
    private final MigrationRunStepRepository stepRepository = mock(MigrationRunStepRepository.class);
    private final MigrationCommandPublisher commandPublisher = mock(MigrationCommandPublisher.class);
    private final CacheService cacheService = mock(CacheService.class);
    private final TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final MigrationSagaOrchestrator orchestrator = new MigrationSagaOrchestrator(cacheService, commandPublisher, runRepository, stepRepository,
        transactionTemplate, Duration.ofHours(2), 2);

    private final List<MigrationRun> runs = new ArrayList<>();
    private final List<MigrationRunStep> steps = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(runRepository.save(any())).thenAnswer(invocation -> {
            MigrationRun run = invocation.getArgument(0);
            if (!runs.contains(run)) {
                runs.add(run);
            }
            return run;
        });
        when(runRepository.existsByStatus(any())).thenAnswer(invocation -> runs.stream().anyMatch(r -> r.getStatus() == invocation.getArgument(0)));
        when(runRepository.findById(any())).thenAnswer(invocation -> runs.stream().filter(r -> r.getId().equals(invocation.getArgument(0))).findFirst());
        when(stepRepository.saveAll(any())).thenAnswer(invocation -> {
            List<MigrationRunStep> saved = new ArrayList<>((List<MigrationRunStep>) invocation.getArgument(0));
            steps.addAll(saved);
            return saved;
        });
        when(stepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.findAllByMigrationIdOrderByStepOrder(any())).thenAnswer(invocation -> steps.stream()
            .filter(s -> s.getMigrationId().equals(invocation.getArgument(0)))
            .sorted(Comparator.comparingInt(MigrationRunStep::getStepOrder))
            .toList());
        when(stepRepository.findByMigrationIdAndStepOrderForUpdate(any(), anyInt())).thenAnswer(invocation -> steps.stream()
            .filter(s -> s.getMigrationId().equals(invocation.getArgument(0)) && s.getStepOrder() == (int) invocation.getArgument(1))
            .findFirst());
        when(stepRepository.findAllByStatusInAndUpdatedAtBefore(any(), any())).thenAnswer(invocation -> {
            Collection<MigrationStepStatus> statuses = invocation.getArgument(0);
            Instant before = invocation.getArgument(1);
            return steps.stream().filter(s -> statuses.contains(s.getStatus()) && s.getUpdatedAt().isBefore(before)).toList();
        });
        when(stepRepository.updateProgress(any(), anyInt(), any(), anyLong(), anyLong(), any(), any())).thenAnswer(invocation -> {
            UUID migrationId = invocation.getArgument(0);
            int stepOrder = invocation.getArgument(1);
            String service = invocation.getArgument(2);
            long processed = invocation.getArgument(3);
            long total = invocation.getArgument(4);
            Instant now = invocation.getArgument(5);
            Collection<MigrationStepStatus> inFlight = invocation.getArgument(6);
            List<MigrationRunStep> matching = steps.stream()
                .filter(s -> s.getMigrationId().equals(migrationId) && s.getStepOrder() == stepOrder && s.getService().equals(service)
                    && inFlight.contains(s.getStatus()))
                .toList();
            matching.forEach(s -> {
                s.setProcessed(processed);
                s.setTotal(total);
                s.setUpdatedAt(now);
            });
            return matching.size();
        });
        when(stepRepository.markRunningIfSent(any(), anyInt())).thenAnswer(invocation -> {
            UUID migrationId = invocation.getArgument(0);
            int stepOrder = invocation.getArgument(1);
            List<MigrationRunStep> matching = steps.stream()
                .filter(s -> s.getMigrationId().equals(migrationId) && s.getStepOrder() == stepOrder && s.getStatus() == MigrationStepStatus.SENT)
                .toList();
            matching.forEach(s -> s.setStatus(MigrationStepStatus.RUNNING));
            return matching.size();
        });
    }

    @Test
    void startsAllServicesInCanonicalOrderWithDefaultSettings() {
        UUID migrationId = orchestrator.start(new MigrationStartRequest(null, null), 7L);

        assertThat(steps).extracting(MigrationRunStep::getService).containsExactlyElementsOf(ALL_SERVICES);
        assertThat(steps).extracting(MigrationRunStep::getStepOrder).containsExactly(1, 2, 3, 4, 5, 6);
        MigrationRun run = runs.getFirst();
        assertThat(run.getId()).isEqualTo(migrationId);
        assertThat(run.getStatus()).isEqualTo(MigrationStatus.RUNNING);
        assertThat(run.getRequestedBy()).isEqualTo(7L);
        assertThat(run.getSettings()).isEqualTo(DEFAULTS);
        assertThat(steps.getFirst().getStatus()).isEqualTo(MigrationStepStatus.SENT);
        assertThat(steps.getFirst().getAttempts()).isEqualTo(1);
        assertThat(sentCommands()).singleElement().satisfies(command -> {
            assertThat(command.migrationId()).isEqualTo(migrationId);
            assertThat(command.step()).isEqualTo(1);
            assertThat(command.service()).isEqualTo("identity-ms");
            assertThat(command.settings()).isEqualTo(DEFAULTS);
        });
    }

    @Test
    void stepsFollowCanonicalOrderNotRequestOrder() {
        start(List.of("ops-ms", "listing-ms"), null);

        assertThat(steps).extracting(MigrationRunStep::getService).containsExactly("listing-ms", "ops-ms");
        assertThat(sentCommands()).extracting(MigrationCommand::service).containsExactly("listing-ms");
    }

    @Test
    void rejectsUnknownService() {
        assertThatThrownBy(() -> start(List.of("ops-ms", "unknown-ms"), null)).isInstanceOf(AppBadRequestException.class);
        assertThatThrownBy(() -> start(Arrays.asList("ops-ms", null), null)).isInstanceOf(AppBadRequestException.class);
        assertThat(runs).isEmpty();
        verify(commandPublisher, never()).publishCommand(any());
    }

    @Test
    void rejectsInvalidSettings() {
        assertThatThrownBy(() -> startWith(new MigrationSettings(MigrationSettings.MAX_BATCH_SIZE + 1, null, null, false, null)))
            .isInstanceOf(AppBadRequestException.class);
        assertThatThrownBy(() -> startWith(new MigrationSettings(-1, null, null, false, null))).isInstanceOf(AppBadRequestException.class);
        assertThatThrownBy(() -> startWith(new MigrationSettings(100, 0L, null, false, null))).isInstanceOf(AppBadRequestException.class);
        assertThat(runs).isEmpty();
        verify(commandPublisher, never()).publishCommand(any());
    }

    @Test
    void zeroBatchSizeUsesDefaultAndKeepsOtherSettings() {
        startWith(new MigrationSettings(0, 500L, null, true, List.of("listings")));

        assertThat(runs.getFirst().getSettings()).isEqualTo(new MigrationSettings(MigrationSettings.DEFAULT_BATCH_SIZE, 500L, null, true, List.of("listings")));
    }

    @Test
    void secondMigrationIsRejectedWhileOneIsRunning() {
        start(List.of("ops-ms"), null);

        assertThatThrownBy(() -> start(List.of("ops-ms"), null))
            .isInstanceOf(AppException.class)
            .extracting("errorCode").isEqualTo(InternalExceptionCode.EX409_JOB_ALREADY_RUNNING);
        assertThat(runs).hasSize(1);
    }

    @Test
    void advancesOnSuccessAndCompletesRun() {
        UUID migrationId = start(List.of("listing-ms", "ops-ms"), null);

        orchestrator.onReply(reply(migrationId, 1, "listing-ms", true, null), t0.plusSeconds(1));
        assertThat(sentCommands()).extracting(MigrationCommand::service).containsExactly("listing-ms", "ops-ms");

        orchestrator.onReply(reply(migrationId, 2, "ops-ms", true, null), t0.plusSeconds(2));
        assertThat(runs.getFirst().getStatus()).isEqualTo(MigrationStatus.COMPLETED);
        assertThat(runs.getFirst().getFinishedAt()).isEqualTo(t0.plusSeconds(2));
        assertThat(steps).extracting(MigrationRunStep::getStatus).containsExactly(MigrationStepStatus.COMPLETED, MigrationStepStatus.COMPLETED);
        assertThat(steps.getFirst().getTables()).isEqualTo(TABLES);
    }

    @Test
    void failureFailsRunAndSkipsRemainingSteps() {
        UUID migrationId = start(List.of("listing-ms", "processing-ms", "ops-ms"), null);

        orchestrator.onReply(reply(migrationId, 1, "listing-ms", true, null), t0.plusSeconds(1));
        orchestrator.onReply(reply(migrationId, 2, "processing-ms", false, "disk full"), t0.plusSeconds(2));

        assertThat(runs.getFirst().getStatus()).isEqualTo(MigrationStatus.FAILED);
        assertThat(steps).extracting(MigrationRunStep::getStatus).containsExactly(
            MigrationStepStatus.COMPLETED, MigrationStepStatus.FAILED, MigrationStepStatus.SKIPPED);
        assertThat(steps.get(1).getError()).isEqualTo("disk full");
        assertThat(sentCommands()).extracting(MigrationCommand::service).containsExactly("listing-ms", "processing-ms");
    }

    @Test
    void duplicateReplyIsIgnored() {
        UUID migrationId = start(List.of("listing-ms", "ops-ms"), null);
        MigrationReply reply = reply(migrationId, 1, "listing-ms", true, null);

        orchestrator.onReply(reply, t0.plusSeconds(1));
        orchestrator.onReply(reply, t0.plusSeconds(2));

        assertThat(sentCommands()).hasSize(2);
        assertThat(steps.getFirst().getFinishedAt()).isEqualTo(t0.plusSeconds(1));
    }

    @Test
    void replyFromAnotherServiceIsIgnored() {
        UUID migrationId = start(List.of("listing-ms", "ops-ms"), null);

        orchestrator.onReply(reply(migrationId, 1, "ops-ms", true, null), t0.plusSeconds(1));

        assertThat(steps.getFirst().getStatus()).isEqualTo(MigrationStepStatus.SENT);
        assertThat(sentCommands()).hasSize(1);
    }

    @Test
    void cancelStopsRunAndLateReplyDoesNotAdvance() {
        UUID migrationId = start(List.of("listing-ms", "ops-ms"), null);

        orchestrator.cancel(migrationId);

        assertThat(runs.getFirst().getStatus()).isEqualTo(MigrationStatus.CANCELLED);
        assertThat(steps).extracting(MigrationRunStep::getStatus).containsExactly(MigrationStepStatus.CANCELLED, MigrationStepStatus.CANCELLED);
        verify(cacheService).put(CacheType.MIGRATION_CANCELLED, migrationId, Boolean.TRUE, Duration.ofDays(1));

        orchestrator.onReply(reply(migrationId, 1, "listing-ms", true, null), t0.plusSeconds(1));

        assertThat(sentCommands()).hasSize(1);
        assertThat(steps.getFirst().getStatus()).isEqualTo(MigrationStepStatus.CANCELLED);
    }

    @Test
    void cancelOfFinishedRunIsConflict() {
        UUID migrationId = start(List.of("ops-ms"), null);
        orchestrator.onReply(reply(migrationId, 1, "ops-ms", true, null), t0.plusSeconds(1));

        assertThatThrownBy(() -> orchestrator.cancel(migrationId))
            .isInstanceOf(AppException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.CONFLICT);
        verify(cacheService, never()).put(any(), any(), any(), any());
    }

    @Test
    void cancelOfUnknownRunIsNotFound() {
        assertThatThrownBy(() -> orchestrator.cancel(UUID.randomUUID())).isInstanceOf(AppNotFoundException.class);
    }

    @Test
    void resendsSilentStepAndTimesOutAfterMaxAttempts() {
        start(List.of("listing-ms", "ops-ms"), null);

        orchestrator.checkTimeouts(t0.plus(Duration.ofHours(2)).plus(Duration.ofMinutes(1)));
        assertThat(steps.getFirst().getAttempts()).isEqualTo(2);

        orchestrator.checkTimeouts(t0.plus(Duration.ofHours(4)).plus(Duration.ofMinutes(2)));

        assertThat(steps.getFirst().getStatus()).isEqualTo(MigrationStepStatus.TIMED_OUT);
        assertThat(steps.get(1).getStatus()).isEqualTo(MigrationStepStatus.SKIPPED);
        assertThat(runs.getFirst().getStatus()).isEqualTo(MigrationStatus.FAILED);
        assertThat(sentCommands()).extracting(MigrationCommand::service).containsExactly("listing-ms", "listing-ms");
    }

    @Test
    void progressRefreshesUpdatedAtAndKeepsStepAliveAgainstTimeout() {
        UUID migrationId = start(List.of("listing-ms", "ops-ms"), null);

        orchestrator.onProgress(new MigrationProgress(migrationId, 1, "listing-ms", "listings", 40, 100), t0.plus(Duration.ofHours(1)));
        orchestrator.onProgress(new MigrationProgress(migrationId, 1, "listing-ms", "listings", 60, 100), t0.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(5)));

        assertThat(steps.getFirst().getStatus()).isEqualTo(MigrationStepStatus.RUNNING);
        assertThat(steps.getFirst().getProcessed()).isEqualTo(60);
        assertThat(steps.getFirst().getTotal()).isEqualTo(100);
        assertThat(steps.getFirst().getUpdatedAt()).isEqualTo(t0.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(5)));

        orchestrator.checkTimeouts(t0.plus(Duration.ofHours(2)).plus(Duration.ofMinutes(30)));

        assertThat(sentCommands()).hasSize(1);
        assertThat(steps.getFirst().getAttempts()).isEqualTo(1);
        assertThat(steps.getFirst().getStatus()).isEqualTo(MigrationStepStatus.RUNNING);
    }

    @Test
    void progressForUnknownRunChangesNothing() {
        start(List.of("ops-ms"), null);

        orchestrator.onProgress(new MigrationProgress(UUID.randomUUID(), 1, "ops-ms", "table", 1, 2), t0);

        assertThat(steps.getFirst().getStatus()).isEqualTo(MigrationStepStatus.SENT);
        assertThat(steps.getFirst().getProcessed()).isZero();
        verify(stepRepository, never()).markRunningIfSent(any(), anyInt());
    }

    private UUID start(List<String> services, Long requestedBy) {
        return orchestrator.start(new MigrationStartRequest(services, null), requestedBy);
    }

    private void startWith(MigrationSettings settings) {
        orchestrator.start(new MigrationStartRequest(List.of("ops-ms"), settings), 1L);
    }

    private List<MigrationCommand> sentCommands() {
        ArgumentCaptor<MigrationCommand> commands = ArgumentCaptor.forClass(MigrationCommand.class);
        verify(commandPublisher, atLeast(0)).publishCommand(commands.capture());
        return commands.getAllValues();
    }

    private static MigrationReply reply(UUID migrationId, int step, String service, boolean success, String error) {
        return new MigrationReply(migrationId, step, service, success, TABLES, error);
    }
}
