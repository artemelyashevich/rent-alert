package com.rentalert.ops_ms.repository;

import com.rentalert.ops_ms.domain.CleanupSagaStep;
import com.rentalert.ops_ms.domain.CleanupStepStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupSagaStepRepository extends JpaRepository<CleanupSagaStep, Long> {

    List<CleanupSagaStep> findAllBySagaIdOrderByStepOrder(UUID sagaId);

    Optional<CleanupSagaStep> findBySagaIdAndStepOrder(UUID sagaId, int stepOrder);

    List<CleanupSagaStep> findAllByStatusAndSentAtBefore(CleanupStepStatus status, Instant sentBefore);
}
