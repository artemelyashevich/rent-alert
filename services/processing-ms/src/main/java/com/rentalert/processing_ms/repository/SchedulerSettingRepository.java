package com.rentalert.processing_ms.repository;

import com.rentalert.processing_ms.domain.SchedulerSettings;
import com.rentalert.dto.listing.ScheduleProvider;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchedulerSettingRepository extends JpaRepository<@NonNull SchedulerSettings, @NonNull Long> {

    Optional<SchedulerSettings> findByProviderName(ScheduleProvider scheduleProvider);
}
