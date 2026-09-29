package com.rentalert.dto.event;

import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobScope;
import com.rentalert.dto.event.job.JobStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One finished run of a background job, sent to {@link KafkaTopics#JOB_EVENTS} with key = jobName.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionEvent extends AbstractEvent {

    private JobName jobName;
    private JobInitiator initiator;
    private JobScope executionScope;
    private String description;
    private Long affectedRecords;
    private Instant startedAt;
    private Instant finishedAt;
    private Long executionTimeMs;
    private JobStatus status;
    private String errorMessage;

    @Override
    public EventInitiatorType getEventType() {
        return initiator == JobInitiator.ADMIN ? EventInitiatorType.ADMIN : EventInitiatorType.INTERNAL;
    }
}
