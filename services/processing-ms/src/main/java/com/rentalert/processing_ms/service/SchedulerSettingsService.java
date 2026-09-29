package com.rentalert.processing_ms.service;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.listing.SchedulerSettingsResponse;

public interface SchedulerSettingsService {

    SchedulerSettingsResponse findByProvider(ScheduleProvider scheduleProvider);
}
