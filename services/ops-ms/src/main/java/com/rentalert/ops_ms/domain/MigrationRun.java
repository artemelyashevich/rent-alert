package com.rentalert.ops_ms.domain;

import com.rentalert.dto.migration.MigrationSettings;
import com.rentalert.dto.migration.MigrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "migration_run")
public class MigrationRun {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MigrationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false, columnDefinition = "jsonb")
    private MigrationSettings settings;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public static MigrationRun start(UUID id, MigrationSettings settings, Long requestedBy, Instant now) {
        MigrationRun run = new MigrationRun();
        run.setId(id);
        run.setStatus(MigrationStatus.RUNNING);
        run.setSettings(settings);
        run.setRequestedBy(requestedBy);
        run.setCreatedAt(now);
        run.setStartedAt(now);
        return run;
    }
}
