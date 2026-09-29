package com.rentalert.ops_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobScope;
import com.rentalert.dto.event.job.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_setting")
public class JobSetting extends AbstractEntity {

    @Column(name = "event_id", unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_name")
    private JobName jobName;

    @Enumerated(EnumType.STRING)
    private JobInitiator initiator;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_scope")
    private JobScope executionScope;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "affected_records", nullable = false)
    private Long affectedRecords;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}