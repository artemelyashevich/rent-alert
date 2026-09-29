package com.rentalert.ops_ms.service;

import com.rentalert.dto.event.JobExecutionEvent;

public interface JobSettingService {

    /**
     * Stores the job run once: a redelivered event (same idempotency key) is ignored.
     */
    void record(JobExecutionEvent event);
}
