package com.rentalert.ops_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.event.job.JobInitiator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cleanup_saga")
public class CleanupSaga extends AbstractEntity {

    @Column(name = "saga_id", nullable = false, unique = true)
    private UUID sagaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CleanupSagaStatus status;

    @Column(name = "reference_time", nullable = false)
    private Instant referenceTime;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "deleted", nullable = false)
    private long deleted;

    @Column(name = "failed", nullable = false)
    private long failed;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiator", nullable = false, length = 20)
    private JobInitiator initiator;

    public static CleanupSaga start(Instant referenceTime, JobInitiator initiator) {
        CleanupSaga saga = new CleanupSaga();
        saga.setSagaId(UUID.randomUUID());
        saga.setStatus(CleanupSagaStatus.RUNNING);
        saga.setReferenceTime(referenceTime);
        saga.setInitiator(initiator);
        return saga;
    }
}
