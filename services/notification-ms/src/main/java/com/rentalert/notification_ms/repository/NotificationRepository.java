package com.rentalert.notification_ms.repository;

import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.notification_ms.domain.Notification;
import com.rentalert.notification_ms.domain.NotificationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<@NonNull Notification, @NonNull Long> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.status IN :statuses
          AND n.attempt < :maxAttempt
          AND n.nextRetryAt <= :now
        ORDER BY n.createdAt ASC
        LIMIT 50
        """)
    List<Notification> findRetryable(
        @Param("statuses") Collection<NotificationStatus> statuses,
        @Param("maxAttempt") int maxAttempt,
        Instant now
    );

    boolean existsByDeduplicationKeyAndStatusIn(
        String deduplicationKey,
        Collection<NotificationStatus> statuses
    );

    List<Notification> findAllByDeduplicationKeyIn(Set<String> deduplicationKey);

    @Modifying
    @Query("""
        delete from Notification where listingId in :listingIds
        """)
    int deleteAllByListingIdIn(@Param("listingIds") List<Long> listingIds);

    @Modifying
    @Query("""
        delete from Notification n
        where n.createdAt < :cutoff
        """)
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);

    @Query("select n.id from Notification n where n.createdAt < :cutoff order by n.id")
    List<Long> findIdsCreatedBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("delete from Notification n where n.id in :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    boolean existsByRequestId(UUID requestId);

    boolean existsByDeduplicationKeyAndEngineAndStatusIn(String deduplicationKey, NotificationEngine engine, List<NotificationStatus> success);

    @Modifying
    @Query("delete from Notification e where e.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
