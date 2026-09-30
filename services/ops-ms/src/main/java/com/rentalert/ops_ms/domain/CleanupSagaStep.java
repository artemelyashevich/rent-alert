package com.rentalert.ops_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
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
@Table(name = "cleanup_saga_step")
public class CleanupSagaStep extends AbstractEntity {

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "service", nullable = false, length = 100)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CleanupStepStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "deleted", nullable = false)
    private long deleted;

    @Column(name = "failed", nullable = false)
    private long failed;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "errors", columnDefinition = "TEXT")
    private String errors;

    public static CleanupSagaStep pending(UUID sagaId, int stepOrder, String service) {
        CleanupSagaStep step = new CleanupSagaStep();
        step.setSagaId(sagaId);
        step.setStepOrder(stepOrder);
        step.setService(service);
        step.setStatus(CleanupStepStatus.PENDING);
        return step;
    }
}
