package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.admin_ms.job.CacheCleanupJob;
import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.job.JobSettingResponse;
import com.rentalert.internal.client.service.OpsMsService;
import com.rentalert.internal.client.service.ProcessingMsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin jobs", description = "Служебные задачи и история их запусков")
@RestController
@RequestMapping(PublicUrls.JOB_SETTINGS_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminJobSettingsController {

    private final OpsMsService opsMsService;
    private final ProcessingMsService processingMsService;
    private final CacheCleanupJob cacheCleanupJob;
    private final AdminAuditPublisher auditPublisher;

    @Operation(summary = "История запусков задач")
    @GetMapping
    public PageResponse<JobSettingResponse> findAll(
        @ParameterObject @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return opsMsService.findJobSettings(pageable);
    }

    @Operation(summary = "Запустить очистку устаревших данных во всех сервисах; возвращает id saga")
    @PostMapping("/cleanUp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UUID startCleanup() {
        UUID sagaId = opsMsService.startCleanup();
        auditPublisher.publish(AdminEvent.JOB_CLEAN_UP);
        return sagaId;
    }

    @Operation(summary = "Удалить все ключи Redis (кэши, сессии, блокировки)")
    @PostMapping("/cleanCache")
    public long cleanCache(@RequestParam(defaultValue = "true") boolean all) {
        if (!all) {
            throw new AppBadRequestException("Unsupported");
        }
        auditPublisher.publish(AdminEvent.JOB_CLEANUP_CACHE_ALL);
        return cacheCleanupJob.run();
    }

    @Operation(summary = "Загрузить свежие курсы валют")
    @PostMapping("/currency-rates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncCurrencyRates() {
        processingMsService.syncCurrencyRates();
        auditPublisher.publish(AdminEvent.JOB_SYNC_CURRENCY_RATES);
    }
}
