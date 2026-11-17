package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateHidden;
import com.rentalert.roommate_ms.domain.RoommateHiddenEmbeddedId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoommateHiddenRepository extends JpaRepository<RoommateHidden, RoommateHiddenEmbeddedId> {

}
