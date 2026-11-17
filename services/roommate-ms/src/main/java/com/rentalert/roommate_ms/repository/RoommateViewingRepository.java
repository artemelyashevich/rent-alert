package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateViewing;
import com.rentalert.roommate_ms.domain.RoommateViewingStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoommateViewingRepository extends JpaRepository<RoommateViewing, Long> {

    Optional<RoommateViewing> findFirstByMatchIdAndStatusOrderByViewingDateAsc(Long matchId, RoommateViewingStatus status);
}
