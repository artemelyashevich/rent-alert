package com.rentalert.notification_ms.repository;

import com.rentalert.dto.notification.ReminderIntervalType;
import com.rentalert.dto.notification.ReminderStatus;
import com.rentalert.notification_ms.domain.Reminder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    @Query(nativeQuery = true, value = """
        SELECT *
        FROM reminders
        WHERE status = :status
          AND remind_at <= :now
        ORDER BY remind_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """)
    List<Reminder> findBatchToRemind(
        @Param("status") String status,
        @Param("now") Instant now,
        @Param("limit") int limit
    );

    boolean existsByUserIdAndListingIdAndIntervalTypeAndStatusIn(
        Long userId, Long listingId, ReminderIntervalType intervalType, List<ReminderStatus> statuses
    );

    List<Reminder> findAllByUserIdOrderByRemindAtAsc(Long userId);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("delete from Reminder e where e.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
