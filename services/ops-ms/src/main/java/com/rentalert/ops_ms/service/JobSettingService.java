package com.rentalert.ops_ms.service;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.job.JobSettingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobSettingService {

    /**
     * Stores the job run once: a redelivered event (same idempotency key) is ignored.
     */
    void record(JobExecutionEvent event);

    Page<JobSettingResponse> findAll(Pageable pageable);
}
