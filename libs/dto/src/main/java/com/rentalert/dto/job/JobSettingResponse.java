package com.rentalert.dto.job;

import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobScope;
import com.rentalert.dto.event.job.JobStatus;
import java.time.Instant;

public record JobSettingResponse(
    JobName jobName,
    JobInitiator initiator,
    String description,
    Long affectedRecords,
    JobScope executionScope,
    Instant startedAt,
    Instant finishedAt,
    Long executionTimeMs,
    JobStatus status,
    String errorMessage
) {

}
