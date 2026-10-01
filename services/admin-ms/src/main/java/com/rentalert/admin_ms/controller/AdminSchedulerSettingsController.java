package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.listing.SchedulerSettingsRequest;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.internal.client.service.ProcessingMsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin scheduler settings", description = "Настройки парсеров")
@RestController
@RequestMapping(PublicUrls.SCHEDULER_SETTINGS_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSchedulerSettingsController {

    private final ProcessingMsService processingMsService;
    private final AdminAuditPublisher auditPublisher;

    @Operation(summary = "Получить все настройки")
    @GetMapping
    public List<SchedulerSettingsResponse> findAll() {
        return processingMsService.findSchedulerSettings();
    }

    @Operation(summary = "Получить настройки провайдера")
    @GetMapping("/{providerName}")
    public SchedulerSettingsResponse findByProvider(@PathVariable String providerName) {
        return processingMsService.findSchedulerSettings(providerName);
    }

    @Operation(summary = "Создать настройки провайдера")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchedulerSettingsResponse create(@Valid @RequestBody SchedulerSettingsRequest request) {
        SchedulerSettingsResponse settings = processingMsService.createSchedulerSettings(request);
        auditPublisher.publish(AdminEvent.SCHEDULER_SETTINGS_CREATE, request.getProviderName());
        return settings;
    }

    @Operation(summary = "Обновить настройки провайдера")
    @PutMapping
    public SchedulerSettingsResponse update(@Valid @RequestBody SchedulerSettingsRequest request) {
        SchedulerSettingsResponse settings = processingMsService.updateSchedulerSettings(request);
        auditPublisher.publish(AdminEvent.SCHEDULER_SETTINGS_UPDATE, request.getProviderName());
        return settings;
    }

    @Operation(summary = "Удалить настройки провайдера")
    @DeleteMapping("/{providerName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String providerName) {
        processingMsService.deleteSchedulerSettings(providerName);
        auditPublisher.publish(AdminEvent.SCHEDULER_SETTINGS_DELETE, providerName);
    }
}
