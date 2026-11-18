package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.migration.MigrationRunResponse;
import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.MigrationStartRequest;
import com.rentalert.internal.client.IdentityMsClient;
import com.rentalert.internal.client.ListingMsClient;
import com.rentalert.internal.client.NotificationMsClient;
import com.rentalert.internal.client.OpsMsClient;
import com.rentalert.internal.client.ProcessingMsClient;
import com.rentalert.internal.client.RoommateMsClient;
import com.rentalert.internal.client.service.OpsMsService;
import com.rentalert.redis.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin migrations", description = "Перенос данных из БД монолита в микросервисы")
@RestController
@RequestMapping(PublicUrls.MIGRATION_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMigrationController {

    private static final Set<String> MIGRATABLE_SERVICES = Set.of(
        IdentityMsClient.SERVICE_NAME,
        ListingMsClient.SERVICE_NAME,
        ProcessingMsClient.SERVICE_NAME,
        NotificationMsClient.SERVICE_NAME,
        RoommateMsClient.SERVICE_NAME,
        OpsMsClient.SERVICE_NAME
    );

    private final AdminAuditPublisher auditPublisher;
    private final OpsMsService opsMsService;
    private final UserSessionService userSessionService;

    @Operation(summary = "Запустить перенос данных выбранных сервисов; возвращает запуск с id, прогресс — в GET /migrations/{id}")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MigrationRunResponse start(@RequestBody MigrationStartRequest request) {
        validate(request);
        MigrationRunResponse run = opsMsService.startMigration(request, userSessionService.getCurrentUser().getUserId());
        auditPublisher.publish(AdminEvent.MIGRATION_STARTED, request.services(), request.settings().dryRun());
        return run;
    }

    @Operation(summary = "Последние запуски переноса")
    @GetMapping
    public List<MigrationRunResponse> findAll(@RequestParam(defaultValue = "20") int limit) {
        if (limit < 1) {
            throw new AppBadRequestException("Параметр limit должен быть больше 0");
        }
        return opsMsService.listMigrations(limit);
    }

    @Operation(summary = "Статус запуска переноса, прогресс шагов и счётчики по таблицам")
    @GetMapping("/{id}")
    public MigrationRunResponse findById(@PathVariable UUID id) {
        return opsMsService.getMigration(id);
    }

    @Operation(summary = "Отменить запуск переноса")
    @PostMapping("/{id}/cancel")
    public MigrationRunResponse cancel(@PathVariable UUID id) {
        MigrationRunResponse run = opsMsService.cancelMigration(id);
        auditPublisher.publish(AdminEvent.MIGRATION_CANCELLED, id);
        return run;
    }

    private static void validate(MigrationStartRequest request) {
        if (request.services() == null || request.services().isEmpty()) {
            throw new AppBadRequestException("Не выбран ни один сервис для переноса");
        }
        List<String> unknown = request.services().stream()
            .filter(service -> service == null || !MIGRATABLE_SERVICES.contains(service))
            .toList();
        if (!unknown.isEmpty()) {
            throw new AppBadRequestException("Перенос недоступен для сервисов: " + unknown);
        }
        MigrationSettings settings = request.settings();
        if (settings == null) {
            throw new AppBadRequestException("Не заданы настройки переноса");
        }
        if (settings.batchSize() < 0 || settings.batchSize() > MigrationSettings.MAX_BATCH_SIZE) {
            throw new AppBadRequestException("batchSize должен быть от 0 до " + MigrationSettings.MAX_BATCH_SIZE);
        }
        if (settings.limitPerTable() != null && settings.limitPerTable() <= 0) {
            throw new AppBadRequestException("limitPerTable должен быть больше 0");
        }
        if (settings.since() != null && settings.since().isAfter(Instant.now())) {
            throw new AppBadRequestException("since не может быть в будущем");
        }
    }
}
