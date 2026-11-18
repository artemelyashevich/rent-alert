package com.rentalert.ops_ms.repository;

import com.rentalert.dto.migration.MigrationStatus;
import com.rentalert.ops_ms.domain.MigrationRun;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationRunRepository extends JpaRepository<MigrationRun, UUID> {

    boolean existsByStatus(MigrationStatus status);
}
