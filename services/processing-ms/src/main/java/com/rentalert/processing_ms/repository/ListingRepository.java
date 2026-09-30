package com.rentalert.processing_ms.repository;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.Listing;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<@NonNull Listing, @NonNull Long> {

    boolean existsBySourceAndSourceId(ScheduleProvider source, String sourceId);

    @Query("select l.id from Listing l where l.parsedAt < :cutoff order by l.id")
    List<Long> findIdsParsedBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("delete from Listing l where l.id in :ids")
    int deleteByIdIn(@Param("ids") List<Long> ids);
}
