package com.rentalert.processing_ms.repository;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.ScheduleTask;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleTaskRepository extends JpaRepository<@NonNull ScheduleTask, @NonNull Long> {

    @Query(
        """
            select schedule from ScheduleTask schedule where schedule.provider = :provider
            """
    )
    Optional<ScheduleTask> findByProvider(@Param("provider") ScheduleProvider scheduleProvider);
}
