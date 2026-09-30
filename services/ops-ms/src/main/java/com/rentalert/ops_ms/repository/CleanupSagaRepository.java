package com.rentalert.ops_ms.repository;

import com.rentalert.ops_ms.domain.CleanupSaga;
import com.rentalert.ops_ms.domain.CleanupSagaStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupSagaRepository extends JpaRepository<CleanupSaga, Long> {

    Optional<CleanupSaga> findBySagaId(UUID sagaId);

    boolean existsByStatus(CleanupSagaStatus status);
}
