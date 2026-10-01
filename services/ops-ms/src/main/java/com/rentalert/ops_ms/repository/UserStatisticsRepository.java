package com.rentalert.ops_ms.repository;

import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.ops_ms.domain.UserStatistics;
import com.rentalert.ops_ms.repository.projection.EventTypeCountProjection;
import com.rentalert.ops_ms.repository.projection.UserActivityCountProjection;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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

    @Query("select s.id from UserStatistics s where s.eventDate < :cutoff order by s.id")
    List<Long> findIdsByEventDateBefore(@Param("cutoff") LocalDate cutoff, Pageable pageable);

    @Modifying
    @Query("delete from UserStatistics s where s.id in :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);

    @Modifying
    @Query("delete from UserStatistics s where s.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Query("""
        select coalesce(sum(s.count), 0) from UserStatistics s
        where s.userId = :userId and s.filterId is null and s.eventType = :eventType and s.eventDate = :date
        """)
    long countWithoutFilter(@Param("userId") Long userId, @Param("eventType") UserEvent eventType, @Param("date") LocalDate date);

    @Query("""
        select coalesce(sum(s.count), 0) from UserStatistics s
        where s.userId = :userId and s.filterId is not null and s.eventType = :eventType and s.eventDate = :date
        """)
    long countWithFilter(@Param("userId") Long userId, @Param("eventType") UserEvent eventType, @Param("date") LocalDate date);

    @Query("""
        select coalesce(avg(s.count), 0.0) from UserStatistics s
        where s.userId = :userId and s.filterId = :filterId and s.eventType = :eventType
          and s.eventDate between :startDate and :endDate
        """)
    double averageByFilter(@Param("userId") Long userId, @Param("filterId") Long filterId, @Param("eventType") UserEvent eventType,
        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        select coalesce(sum(s.count), 0) from UserStatistics s
        where s.userId = :userId and s.eventType in :eventTypes
          and s.eventDate >= :startDate and s.eventDate <= :endDate
        """)
    long countInRange(@Param("userId") Long userId, @Param("eventTypes") Collection<UserEvent> eventTypes,
        @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        select max(s.eventDate) from UserStatistics s
        where s.userId = :userId and s.eventDate >= :since
        """)
    Optional<LocalDate> findLastActivityDate(@Param("userId") Long userId, @Param("since") LocalDate since);

    @Query("""
        select s from UserStatistics s
        where s.userId = :userId and s.eventDate >= :since and s.eventDate <= :until
        order by s.eventDate desc, s.id desc
        """)
    List<UserStatistics> findActivity(@Param("userId") Long userId, @Param("since") LocalDate since, @Param("until") LocalDate until);

    @Query("""
        select s.userId as userId, count(s) as actionCount from UserStatistics s
        where s.eventDate >= :since
        group by s.userId
        order by count(s) desc
        """)
    List<UserActivityCountProjection> findMostActiveUsers(@Param("since") LocalDate since, Pageable pageable);

    @Query("""
        select s.eventType as eventType, coalesce(sum(s.count), 0) as total from UserStatistics s
        where s.eventDate >= :since and s.eventDate <= :until and s.eventType in :eventTypes
        group by s.eventType
        """)
    List<EventTypeCountProjection> findEventDistribution(@Param("since") LocalDate since, @Param("until") LocalDate until,
        @Param("eventTypes") Collection<UserEvent> eventTypes);

    @Query("""
        select count(distinct s.userId) from UserStatistics s
        where s.eventDate >= :since and s.eventDate <= :until
        """)
    long countDistinctUsers(@Param("since") LocalDate since, @Param("until") LocalDate until);

    @Query("""
        select coalesce(sum(s.count), 0) from UserStatistics s
        where s.eventType = :eventType and s.eventDate >= :since and s.eventDate <= :until
        """)
    long countTotal(@Param("eventType") UserEvent eventType, @Param("since") LocalDate since, @Param("until") LocalDate until);
}
