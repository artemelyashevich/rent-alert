package com.rentalert.processing_ms.repository;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.Listing;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<@NonNull Listing, @NonNull Long> {

    boolean existsBySourceAndSourceId(ScheduleProvider source, String sourceId);
}
