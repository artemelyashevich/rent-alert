package com.rentalert.core_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@Entity
@Table(name = "schedule_tasks")
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ScheduleTask extends AbstractEntity {

    @Column(name = "last_processed_id")
    private String lastProcessedId;

    @Builder.Default
    @Column(nullable = false)
    private Long attempt = 1L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.rentalert.dto.listing.ScheduleProvider provider;

    @Builder.Default
    @Column(nullable = false)
    private boolean scrap = false;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;
}