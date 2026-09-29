package com.rentalert.listing_ms.repository;

import com.rentalert.listing_ms.domain.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingStatusRepository extends JpaRepository<ListingStatus, Long> {
}
