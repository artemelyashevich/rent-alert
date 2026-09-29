package com.rentalert.ops_ms.repository;

import com.rentalert.ops_ms.domain.JobSetting;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSettingRepository extends JpaRepository<JobSetting, Long> {

    boolean existsByEventId(UUID eventId);
}
