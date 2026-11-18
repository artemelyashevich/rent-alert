package com.rentalert.ops_ms.repository;

import com.rentalert.ops_ms.domain.CleanupSagaStep;
import com.rentalert.ops_ms.domain.CleanupStepStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CleanupSagaStepRepository extends JpaRepository<CleanupSagaStep, Long> {

    List<CleanupSagaStep> findAllBySagaIdOrderByStepOrder(UUID sagaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CleanupSagaStep s where s.sagaId = :sagaId and s.stepOrder = :stepOrder")
    Optional<CleanupSagaStep> findBySagaIdAndStepOrderForUpdate(@Param("sagaId") UUID sagaId, @Param("stepOrder") int stepOrder);

    List<CleanupSagaStep> findAllByStatusAndSentAtBefore(CleanupStepStatus status, Instant sentBefore);
}
