package com.rentalert.processing_ms.controller;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.listing.SchedulerSettingsRequest;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.processing_ms.config.PublicUrls;
import com.rentalert.processing_ms.service.SchedulerSettingsService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG + "/scheduler-settings")
@RequiredArgsConstructor
public class AdminSchedulerSettingsInternalController {

    private final SchedulerSettingsService schedulerSettingsService;

    @GetMapping
    public List<SchedulerSettingsResponse> findAll() {
        return schedulerSettingsService.findAll();
    }

    @GetMapping("/{providerName}")
    public SchedulerSettingsResponse findByProvider(@PathVariable String providerName) {
        return schedulerSettingsService.findByProvider(ScheduleProvider.getScheduleProvider(providerName));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchedulerSettingsResponse create(@Valid @RequestBody SchedulerSettingsRequest request) {
        return schedulerSettingsService.create(request);
    }

    @PutMapping
    public SchedulerSettingsResponse update(@Valid @RequestBody SchedulerSettingsRequest request) {
        return schedulerSettingsService.update(request);
    }

    @DeleteMapping("/{providerName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String providerName) {
        schedulerSettingsService.delete(ScheduleProvider.getScheduleProvider(providerName));
    }
}
