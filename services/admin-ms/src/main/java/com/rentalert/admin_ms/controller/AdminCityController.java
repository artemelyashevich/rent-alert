package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.listing.CityRequest;
import com.rentalert.dto.listing.CityResponse;
import com.rentalert.dto.listing.CityUpdateRequest;
import com.rentalert.internal.client.service.ProcessingMsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin cities", description = "Справочник городов парсеров")
@RestController
@RequestMapping(PublicUrls.CITY_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCityController {

    private final ProcessingMsService processingMsService;
    private final AdminAuditPublisher auditPublisher;

    @Operation(summary = "Получить города")
    @GetMapping
    public PageResponse<CityResponse> findAll(
        @ParameterObject @PageableDefault(size = 20, sort = {"provider", "name"}, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return processingMsService.findCities(pageable);
    }

    @Operation(summary = "Получить город по ID")
    @GetMapping("/{id}")
    public CityResponse findById(@PathVariable Long id) {
        return processingMsService.findCity(id);
    }

    @Operation(summary = "Создать город")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse create(@Valid @RequestBody CityRequest request) {
        CityResponse city = processingMsService.createCity(request);
        auditPublisher.publish(AdminEvent.CITY_CREATE, request.getName(), request.getProvider().name());
        return city;
    }

    @Operation(summary = "Обновить город")
    @PutMapping("/{id}")
    public CityResponse update(@PathVariable Long id, @Valid @RequestBody CityUpdateRequest request) {
        CityResponse city = processingMsService.updateCity(id, request);
        auditPublisher.publish(AdminEvent.CITY_UPDATE, id);
        return city;
    }

    @Operation(summary = "Изменить флаг активности города")
    @PatchMapping("/{id}/active")
    public CityResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        CityResponse city = processingMsService.setCityActive(id, active);
        auditPublisher.publish(AdminEvent.CITY_TOGGLE_ACTIVE, id, active);
        return city;
    }

    @Operation(summary = "Удалить город")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        processingMsService.deleteCity(id);
        auditPublisher.publish(AdminEvent.CITY_DELETE, id);
    }
}
