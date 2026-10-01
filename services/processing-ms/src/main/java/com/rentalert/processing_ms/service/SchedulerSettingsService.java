package com.rentalert.processing_ms.service;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.listing.SchedulerSettingsRequest;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import java.util.List;

public interface SchedulerSettingsService {

    SchedulerSettingsResponse create(SchedulerSettingsRequest request);

    SchedulerSettingsResponse update(SchedulerSettingsRequest request);

    SchedulerSettingsResponse findByProvider(ScheduleProvider scheduleProvider);

    List<SchedulerSettingsResponse> findAll();

    void delete(ScheduleProvider provider);
}
