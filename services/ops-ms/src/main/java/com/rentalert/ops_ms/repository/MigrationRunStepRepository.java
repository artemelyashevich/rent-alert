package com.rentalert.ops_ms.repository;

import com.rentalert.dto.migration.MigrationStepStatus;
import com.rentalert.ops_ms.domain.MigrationRunStep;
import com.rentalert.ops_ms.domain.MigrationRunStepKey;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MigrationRunStepRepository extends JpaRepository<MigrationRunStep, MigrationRunStepKey> {

    List<MigrationRunStep> findAllByMigrationIdOrderByStepOrder(UUID migrationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MigrationRunStep s where s.migrationId = :migrationId and s.stepOrder = :stepOrder")
    Optional<MigrationRunStep> findByMigrationIdAndStepOrderForUpdate(@Param("migrationId") UUID migrationId, @Param("stepOrder") int stepOrder);

    List<MigrationRunStep> findAllByStatusInAndUpdatedAtBefore(Collection<MigrationStepStatus> statuses, Instant updatedBefore);

    @Modifying
    @Query("update MigrationRunStep s set s.processed = :processed, s.total = :total, s.updatedAt = :now "
        + "where s.migrationId = :migrationId and s.stepOrder = :stepOrder and s.service = :service and s.status in :inFlight")
    int updateProgress(@Param("migrationId") UUID migrationId, @Param("stepOrder") int stepOrder, @Param("service") String service,
                       @Param("processed") long processed, @Param("total") long total, @Param("now") Instant now,
                       @Param("inFlight") Collection<MigrationStepStatus> inFlight);

    @Modifying
    @Query("update MigrationRunStep s set s.status = com.rentalert.dto.migration.MigrationStepStatus.RUNNING "
        + "where s.migrationId = :migrationId and s.stepOrder = :stepOrder and s.status = com.rentalert.dto.migration.MigrationStepStatus.SENT")
    int markRunningIfSent(@Param("migrationId") UUID migrationId, @Param("stepOrder") int stepOrder);
}
