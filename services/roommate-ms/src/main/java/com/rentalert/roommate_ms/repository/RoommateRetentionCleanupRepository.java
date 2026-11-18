package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateMatch;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface RoommateRetentionCleanupRepository extends Repository<RoommateMatch, Long> {

    @Query(value = """
        SELECT e.id
        FROM roommate_match_events e
        JOIN roommate_matches m ON m.id = e.match_id
        WHERE m.status IN ('DECLINED', 'EXPIRED', 'ARCHIVED')
          AND e.created_at < :cutoff
        ORDER BY e.id
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findStaleMatchEventIds(@Param("cutoff") Instant cutoff, @Param("limit") int limit);

    @Modifying
    @Query(value = "DELETE FROM roommate_match_events WHERE id IN (:ids)", nativeQuery = true)
    int deleteMatchEventsByIdIn(@Param("ids") Collection<Long> ids);

    @Query(value = """
        SELECT v.id
        FROM roommate_viewings v
        JOIN roommate_matches m ON m.id = v.match_id
        WHERE m.status IN ('DECLINED', 'EXPIRED', 'ARCHIVED')
          AND v.status IN ('CANCELLED', 'DONE')
          AND v.created_at < :cutoff
        ORDER BY v.id
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findStaleViewingIds(@Param("cutoff") Instant cutoff, @Param("limit") int limit);

    @Modifying
    @Query(value = "DELETE FROM roommate_viewings WHERE id IN (:ids)", nativeQuery = true)
    int deleteViewingsByIdIn(@Param("ids") Collection<Long> ids);

    @Query(value = """
        SELECT id
        FROM roommate_moderation_tasks
        WHERE status IN ('APPROVED_AUTO', 'APPROVED', 'REJECTED_AUTO', 'REJECTED')
          AND updated_at < :cutoff
        ORDER BY id
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findResolvedModerationTaskIds(@Param("cutoff") Instant cutoff, @Param("limit") int limit);

    @Modifying
    @Query(value = "DELETE FROM roommate_moderation_tasks WHERE id IN (:ids)", nativeQuery = true)
    int deleteModerationTasksByIdIn(@Param("ids") Collection<Long> ids);
}
