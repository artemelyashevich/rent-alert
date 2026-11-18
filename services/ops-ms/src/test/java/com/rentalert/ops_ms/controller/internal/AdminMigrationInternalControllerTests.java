package com.rentalert.ops_ms.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.migration.MigrationRunResponse;
import com.rentalert.dto.migration.MigrationStartRequest;
import com.rentalert.dto.migration.MigrationStatus;
import com.rentalert.ops_ms.migration.MigrationRunQueryService;
import com.rentalert.ops_ms.migration.MigrationSagaOrchestrator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

class AdminMigrationInternalControllerTests {

    private final MigrationRunQueryService queryService = mock(MigrationRunQueryService.class);
    private final MigrationSagaOrchestrator orchestrator = mock(MigrationSagaOrchestrator.class);
    private final AdminMigrationInternalController controller = new AdminMigrationInternalController(queryService, orchestrator);

    @Test
    void startReturnsCreatedRun() {
        UUID migrationId = UUID.randomUUID();
        MigrationStartRequest request = new MigrationStartRequest(List.of("ops-ms"), null);
        MigrationRunResponse response = run(migrationId, MigrationStatus.RUNNING);
        when(orchestrator.start(request, 7L)).thenReturn(migrationId);
        when(queryService.get(migrationId)).thenReturn(response);

        assertThat(controller.start(request, 7L)).isEqualTo(response);
    }

    @Test
    void runningMigrationIsConflict() {
        when(orchestrator.start(any(), any())).thenThrow(new AppException(InternalExceptionCode.EX409_JOB_ALREADY_RUNNING, HttpStatus.CONFLICT));

        assertThatThrownBy(() -> controller.start(new MigrationStartRequest(null, null), 7L))
            .isInstanceOf(AppException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cancelCancelsAndReturnsRun() {
        UUID migrationId = UUID.randomUUID();
        MigrationRunResponse response = run(migrationId, MigrationStatus.CANCELLED);
        when(queryService.get(migrationId)).thenReturn(response);

        assertThat(controller.cancel(migrationId)).isEqualTo(response);
        verify(orchestrator).cancel(migrationId);
    }

    @Test
    void getReturnsRun() {
        UUID migrationId = UUID.randomUUID();
        MigrationRunResponse response = run(migrationId, MigrationStatus.COMPLETED);
        when(queryService.get(migrationId)).thenReturn(response);

        assertThat(controller.get(migrationId)).isEqualTo(response);
    }

    @Test
    void listDelegatesToQueryService() {
        Pageable pageable = PageRequest.of(0, 20);
        PageResponse<MigrationRunResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0);
        when(queryService.findAll(pageable)).thenReturn(page);

        assertThat(controller.findAll(pageable)).isEqualTo(page);
    }

    private static MigrationRunResponse run(UUID id, MigrationStatus status) {
        return new MigrationRunResponse(id, status, null, null, null, null, 7L, List.of());
    }
}
