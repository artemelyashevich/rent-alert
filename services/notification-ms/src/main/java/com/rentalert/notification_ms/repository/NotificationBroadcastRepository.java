package com.rentalert.notification_ms.repository;

import com.rentalert.notification_ms.domain.NotificationBroadcast;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationBroadcastRepository extends JpaRepository<@NonNull NotificationBroadcast, @NonNull Long> {

    @Query("""
            select b
            from NotificationBroadcast b
            left join fetch b.userId
            order by b.createdAt desc
        """)
    Page<NotificationBroadcast> findAllWithCreatedBy(Pageable pageable);

    @Query("""
            select b
            from NotificationBroadcast b
            left join fetch b.userId
            where b.id = :id
        """)
    Optional<NotificationBroadcast> findByIdWithCreatedBy(@Param("id") Long id);
}
