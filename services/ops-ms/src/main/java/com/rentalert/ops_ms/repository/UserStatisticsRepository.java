package com.rentalert.ops_ms.repository;

import com.rentalert.ops_ms.domain.UserStatistics;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

    @Modifying
    @Query(nativeQuery = true, value = """
        INSERT INTO user_statistics (user_id, filter_id, event_type, event_date, count)
        VALUES (:userId, :filterId, :eventType, :eventDate, 1)
        ON CONFLICT (user_id, filter_id, event_type, event_date)
        DO UPDATE SET count = user_statistics.count + 1, updated_at = now()
        """)
    void increment(
        @Param("userId") Long userId,
        @Param("filterId") Long filterId,
        @Param("eventType") String eventType,
        @Param("eventDate") LocalDate eventDate
    );
}
