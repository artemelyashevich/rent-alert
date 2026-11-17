package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateMatchParticipant;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoommateMatchParticipantRepository extends JpaRepository<RoommateMatchParticipant, Long> {

    @EntityGraph(attributePaths = {"match"})
    List<RoommateMatchParticipant> findByMatchId(Long matchId);

    @EntityGraph(attributePaths = {"match"})
    @Query("""
        select p from RoommateMatchParticipant p
        where p.userId = :userId
        order by p.match.updatedAt desc
        """)
    Page<RoommateMatchParticipant> findMine(
        @Param("userId") Long userId,
        Pageable pageable);

    @EntityGraph(attributePaths = {"match"})
    @Query("""
        select p from RoommateMatchParticipant p
        where p.userId = :userId
          and p.match.status in :statuses
        order by p.match.updatedAt desc
        """)
    Page<RoommateMatchParticipant> findMineByStatuses(
        @Param("userId") Long userId,
        @Param("statuses") Collection<RoommateMatchStatus> statuses,
        Pageable pageable);
}
