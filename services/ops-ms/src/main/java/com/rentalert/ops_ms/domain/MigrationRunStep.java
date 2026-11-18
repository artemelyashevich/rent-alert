package com.rentalert.ops_ms.domain;

import com.rentalert.dto.migration.MigrationStepStatus;
import com.rentalert.dto.migration.TableResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "migration_run_step")
@IdClass(MigrationRunStepKey.class)
public class MigrationRunStep {

    @Id
    @Column(name = "migration_id", nullable = false)
    private UUID migrationId;

    @Id
    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "service", nullable = false, length = 100)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MigrationStepStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tables", columnDefinition = "jsonb")
    private List<TableResult> tables;

    @Column(name = "processed", nullable = false)
    private long processed;

    @Column(name = "total", nullable = false)
    private long total;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static MigrationRunStep pending(UUID migrationId, int stepOrder, String service, Instant now) {
        MigrationRunStep step = new MigrationRunStep();
        step.setMigrationId(migrationId);
        step.setStepOrder(stepOrder);
        step.setService(service);
        step.setStatus(MigrationStepStatus.PENDING);
        step.setUpdatedAt(now);
        return step;
    }
}
