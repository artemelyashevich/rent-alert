package com.rentalert.ops_ms.repository;

import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.ops_ms.domain.AuditLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        select a from AuditLog a
        where (cast(:eventInitiator as String) is null or a.eventInitiator = :eventInitiator)
          and (cast(:userId as Long) is null or a.userId = :userId)
        """)
    Page<AuditLog> search(
        @Param("eventInitiator") EventInitiatorType eventInitiator,
        @Param("userId") Long userId,
        Pageable pageable
    );

    boolean existsByEventId(UUID eventId);

    @Modifying
    @Query("""
        delete from AuditLog a where a.createdAt < :cutoff
        """)
    int deleteAllByCreatedAtBefore(Instant cutoff);
}
