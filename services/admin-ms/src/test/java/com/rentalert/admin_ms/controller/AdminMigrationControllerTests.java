package com.rentalert.admin_ms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.migration.MigrationRunResponse;
import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.MigrationStartRequest;
import com.rentalert.dto.migration.MigrationStatus;
import com.rentalert.dto.user.UserSession;
import com.rentalert.internal.client.service.OpsMsService;
import com.rentalert.redis.service.UserSessionService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminMigrationControllerTests {

    private final OpsMsService opsMsService = mock(OpsMsService.class);
    private final AdminAuditPublisher auditPublisher = mock(AdminAuditPublisher.class);
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private final AdminMigrationController controller = new AdminMigrationController(auditPublisher, opsMsService, userSessionService);

    private final UUID runId = UUID.randomUUID();
    private final MigrationRunResponse run = new MigrationRunResponse(runId, MigrationStatus.PENDING, null, null, null, null, 99L, List.of());

    private static MigrationStartRequest request(List<String> services, MigrationSettings settings) {
        return new MigrationStartRequest(services, settings);
    }

    private static MigrationSettings settings(int batchSize, Long limitPerTable, Instant since, boolean dryRun) {
        return new MigrationSettings(batchSize, limitPerTable, since, dryRun, null);
    }

    @Test
    void startForwardsRequestWithCurrentAdminAndAudits() {
        MigrationStartRequest request = request(List.of("identity-ms", "ops-ms"), settings(1000, null, null, true));
        when(userSessionService.getCurrentUser()).thenReturn(UserSession.builder().userId(99L).build());
        when(opsMsService.startMigration(request, 99L)).thenReturn(run);

        assertThat(controller.start(request)).isSameAs(run);
        verify(auditPublisher).publish(AdminEvent.MIGRATION_STARTED, List.of("identity-ms", "ops-ms"), true);
    }

    @Test
    void startWithoutServicesIsRejected() {
        assertThatThrownBy(() -> controller.start(request(null, settings(1000, null, null, false))))
            .isInstanceOf(AppBadRequestException.class);
        assertThatThrownBy(() -> controller.start(request(List.of(), settings(1000, null, null, false))))
            .isInstanceOf(AppBadRequestException.class);
        verifyNoInteractions(opsMsService, auditPublisher);
    }

    @Test
    void startWithUnknownOrNullServiceIsRejected() {
        assertThatThrownBy(() -> controller.start(request(List.of("listing-ms", "monolith"), settings(1000, null, null, false))))
            .isInstanceOf(AppBadRequestException.class)
            .hasMessageContaining("monolith");
        assertThatThrownBy(() -> controller.start(request(Arrays.asList("notification-ms", null), settings(1000, null, null, false))))
            .isInstanceOf(AppBadRequestException.class);
        verifyNoInteractions(opsMsService, auditPublisher);
    }

    @Test
    void startWithoutSettingsIsRejected() {
        assertThatThrownBy(() -> controller.start(request(List.of("ops-ms"), null)))
            .isInstanceOf(AppBadRequestException.class);
        verifyNoInteractions(opsMsService, auditPublisher);
    }

    @Test
    void batchSizeOutsideRangeIsRejected() {
        assertThatThrownBy(() -> controller.start(request(List.of("ops-ms"), settings(-1, null, null, false))))
            .isInstanceOf(AppBadRequestException.class);
        assertThatThrownBy(() -> controller.start(request(List.of("ops-ms"),
            settings(MigrationSettings.MAX_BATCH_SIZE + 1, null, null, false))))
            .isInstanceOf(AppBadRequestException.class);
        verifyNoInteractions(opsMsService, auditPublisher);
    }

    @Test
    void limitPerTableMustBePositiveWhenSet() {
        assertThatThrownBy(() -> controller.start(request(List.of("ops-ms"), settings(1000, 0L, null, false))))
            .isInstanceOf(AppBadRequestException.class);
        assertThatThrownBy(() -> controller.start(request(List.of("ops-ms"), settings(1000, -5L, null, false))))
            .isInstanceOf(AppBadRequestException.class);
        verifyNoInteractions(opsMsService, auditPublisher);
    }

    @Test
    void sinceInFutureIsRejected() {
        Instant future = Instant.now().plus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> controller.start(request(List.of("ops-ms"), settings(1000, null, future, false))))
            .isInstanceOf(AppBadRequestException.class);
        verifyNoInteractions(opsMsService, auditPublisher);
    }

    @Test
    void listForwardsLimit() {
        when(opsMsService.listMigrations(20)).thenReturn(List.of(run));

        assertThat(controller.findAll(20)).containsExactly(run);
    }

    @Test
    void listRejectsNonPositiveLimit() {
        assertThatThrownBy(() -> controller.findAll(0)).isInstanceOf(AppBadRequestException.class);
        verify(opsMsService, never()).listMigrations(0);
    }

    @Test
    void statusIsForwarded() {
        when(opsMsService.getMigration(runId)).thenReturn(run);

        assertThat(controller.findById(runId)).isSameAs(run);
    }

    @Test
    void cancellationIsForwardedAndAudited() {
        when(opsMsService.cancelMigration(runId)).thenReturn(run);

        assertThat(controller.cancel(runId)).isSameAs(run);
        verify(auditPublisher).publish(AdminEvent.MIGRATION_CANCELLED, runId);
    }
}
