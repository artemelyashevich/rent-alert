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
            left join fetch r.userProfile u
            where r.broadcast.id = :broadcastId
              and (
                    cast(:search as string) is null
                    or lower(u.username) like lower(concat('%', cast(:search as string), '%'))
                    or (:searchId is not null and u.id = :searchId)
                  )
            order by r.createdAt asc
        """)
    Page<NotificationBroadcastRecipient> findAllByBroadcastId(
        @Param("broadcastId") Long broadcastId,
        @Param("search") String search,
        @Param("searchId") Long searchId,
        Pageable pageable
    );
}
