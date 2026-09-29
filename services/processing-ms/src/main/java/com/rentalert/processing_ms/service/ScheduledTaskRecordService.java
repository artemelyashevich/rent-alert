package com.rentalert.processing_ms.service;

import com.rentalert.dto.listing.ScheduleProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface ScheduledTaskRecordService {

    void markPending(ScheduleProvider provider, String taskKey);

    void markSuccess(ScheduleProvider provider, String taskKey);

    void markError(ScheduleProvider provider, String taskKey, Exception e);

    List<String> getSuccessKeysForWarmUp(ScheduleProvider provider, Duration ttl);

    /** Записи планировщика старше {@code cutoff} (по {@code created_at}). */
    int deleteOlderThan(Instant cutoff);
}
