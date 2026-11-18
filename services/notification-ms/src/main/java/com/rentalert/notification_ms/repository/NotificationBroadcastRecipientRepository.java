package com.rentalert.notification_ms.repository;

import com.rentalert.notification_ms.domain.NotificationBroadcastRecipient;
import java.util.Collection;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationBroadcastRecipientRepository extends JpaRepository<@NonNull NotificationBroadcastRecipient, @NonNull Long> {

    @Query("""
            select r
            from NotificationBroadcastRecipient r
            where r.broadcast.id = :broadcastId
            order by r.createdAt asc
        """)
    Page<NotificationBroadcastRecipient> findAllByBroadcastId(
        @Param("broadcastId") Long broadcastId,
        Pageable pageable
    );

    @Query("""
            select r
            from NotificationBroadcastRecipient r
            where r.broadcast.id = :broadcastId
              and r.userId in :userIds
            order by r.createdAt asc
        """)
    Page<NotificationBroadcastRecipient> findAllByBroadcastIdAndUserIdIn(
        @Param("broadcastId") Long broadcastId,
        @Param("userIds") Collection<Long> userIds,
        Pageable pageable
    );

    @Modifying
    @Query("delete from NotificationBroadcastRecipient e where e.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
