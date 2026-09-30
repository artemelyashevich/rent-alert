package com.rentalert.processing_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
@Table(
    name = "scheduled_task_records",
    indexes = {
        @Index(name = "idx_task_records_processed_at", columnList = "processed_at")
    }
)
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskRecord extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.rentalert.dto.listing.ScheduleProvider provider;

    @Column(name = "task_key", nullable = false)
    private String taskKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScheduledTaskStatus status = ScheduledTaskStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at")
    private Instant processedAt;
}