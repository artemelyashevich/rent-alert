package com.rentalert.processing_ms.repository;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.SchedulerSettings;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerSettingRepository extends JpaRepository<@NonNull SchedulerSettings, @NonNull Long> {

    Optional<SchedulerSettings> findByProviderName(ScheduleProvider scheduleProvider);

    boolean existsByProviderName(ScheduleProvider scheduleProvider);

    void deleteByProviderName(ScheduleProvider scheduleProvider);
}
