package com.rentalert.ops_ms.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.ops_ms.cleanup.CleanupSagaOrchestrator;
import com.rentalert.ops_ms.service.JobSettingService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminJobInternalControllerTests {

    private final CleanupSagaOrchestrator orchestrator = mock(CleanupSagaOrchestrator.class);
    private final AdminJobInternalController controller = new AdminJobInternalController(mock(JobSettingService.class), orchestrator);

    @Test
    void adminStartsCleanupSaga() {
        UUID sagaId = UUID.randomUUID();
        when(orchestrator.start(any(), eq(JobInitiator.ADMIN))).thenReturn(Optional.of(sagaId));

        assertThat(controller.startCleanup()).isEqualTo(sagaId);
    }

    @Test
    void runningCleanupIsConflict() {
        when(orchestrator.start(any(), eq(JobInitiator.ADMIN))).thenReturn(Optional.empty());

        assertThatThrownBy(controller::startCleanup)
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(InternalExceptionCode.EX409_JOB_ALREADY_RUNNING);
    }
}
