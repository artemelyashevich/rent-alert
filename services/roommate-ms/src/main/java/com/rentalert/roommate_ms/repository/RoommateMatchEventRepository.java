package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateMatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoommateMatchEventRepository extends JpaRepository<RoommateMatchEvent, Long> {

}
