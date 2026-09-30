package com.rentalert.notification_ms.repository;

import com.rentalert.notification_ms.domain.NotificationBroadcastRecipient;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationBroadcastRecipientRepository extends JpaRepository<@NonNull NotificationBroadcastRecipient, @NonNull Long> {

    @Query("""
            select r
            from NotificationBroadcastRecipient r
            where r.broadcast.id = :broadcastId
              and (:searchId is null or r.userId = :searchId)
            order by r.createdAt asc
        """)
    Page<NotificationBroadcastRecipient> findAllByBroadcastId(
        @Param("broadcastId") Long broadcastId,
        @Param("searchId") Long searchId,
        Pageable pageable
    );
}
