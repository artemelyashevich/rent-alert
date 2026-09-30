package com.rentalert.processing_ms.repository;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.ScheduledTaskRecord;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledTaskRecordRepository extends JpaRepository<@NonNull ScheduledTaskRecord, @NonNull Long> {

    @Query("""
        SELECT r.taskKey FROM ScheduledTaskRecord r
        WHERE r.provider = :provider
          AND r.status = 'SUCCESS'
          AND r.processedAt >= :since
        """)
    List<String> findSuccessKeysByProviderSince(
        @Param("provider") ScheduleProvider provider,
        @Param("since") Instant since
    );

    @Modifying
    @Query(value = """
        INSERT INTO scheduled_task_records (provider, task_key, status, error_message, processed_at, created_at, updated_at)
        VALUES (:provider, :taskKey, :status, :errorMessage, :processedAt, now(), now())
        ON CONFLICT (provider, task_key)
        DO UPDATE SET
            status        = EXCLUDED.status,
            error_message = EXCLUDED.error_message,
            processed_at  = EXCLUDED.processed_at,
            updated_at    = now()
        """, nativeQuery = true)
    void upsert(
        @Param("provider") String provider,
        @Param("taskKey") String taskKey,
        @Param("status") String status,
        @Param("errorMessage") String errorMessage,
        @Param("processedAt") Instant processedAt
    );

    /**
     * Возрастная зачистка по {@code created_at}, а не по {@code processed_at}: так подчищаются и «застрявшие» PENDING-записи (у них {@code processed_at IS NULL}, и {@code processed_at < cutoff} их
     * никогда не выбирал).
     */
    @Modifying
    @Query("""
        delete from ScheduledTaskRecord s where s.createdAt < :cutoff
        """)
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
