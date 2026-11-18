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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoommateMatchRepository extends JpaRepository<RoommateMatch, Long> {

    Optional<RoommateMatch> findByUserLoIdAndUserHiIdAndScenarioAndContextRef(
        Long userLoId, Long userHiId, RoommateScenario scenario, String contextRef);

    @Modifying(clearAutomatically = false)
    @Query(value = """
        INSERT INTO roommate_matches (scenario, user_lo_id, user_hi_id, context_ref, roommate_listing_id,
            lo_decision, hi_decision, status, status_version, initiator_user_id, expires_at,
            lo_moved, hi_moved, created_at, updated_at)
        VALUES (:scenario, :userLoId, :userHiId, :contextRef, :roommateListingId,
            :loDecision, :hiDecision, :status, 0, :initiatorUserId, :expiresAt,
            FALSE, FALSE, :now, :now)
        ON CONFLICT (user_lo_id, user_hi_id, scenario, context_ref) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("scenario") String scenario,
        @Param("userLoId") Long userLoId,
        @Param("userHiId") Long userHiId,
        @Param("contextRef") String contextRef,
        @Param("roommateListingId") Long roommateListingId,
        @Param("loDecision") String loDecision,
        @Param("hiDecision") String hiDecision,
        @Param("status") String status,
        @Param("initiatorUserId") Long initiatorUserId,
        @Param("expiresAt") Instant expiresAt,
        @Param("now") Instant now);

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
