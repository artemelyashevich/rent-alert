package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateMatch;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoommateMatchRepository extends JpaRepository<RoommateMatch, Long> {

    Optional<RoommateMatch> findByUserLoIdAndUserHiIdAndScenarioAndContextRef(
        Long userLoId, Long userHiId, RoommateScenario scenario, String contextRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from RoommateMatch m where m.id = :id")
    Optional<RoommateMatch> findByIdForUpdate(@Param("id") Long id);

    List<RoommateMatch> findByStatusAndExpiresAtBefore(RoommateMatchStatus status, Instant cutoff);

    @Query("""
        select m from RoommateMatch m
        where m.scenario = :scenario
          and m.id <> :exceptId
          and m.status in :statuses
          and (m.userLoId = :userId or m.userHiId = :userId)
        """)
    List<RoommateMatch> findOtherActive(
        @Param("userId") Long userId,
        @Param("scenario") RoommateScenario scenario,
        @Param("exceptId") Long exceptId,
        @Param("statuses") List<RoommateMatchStatus> statuses);
}
