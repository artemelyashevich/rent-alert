package com.rentalert.listing_ms.repository;

import com.rentalert.listing_ms.domain.HiddenListing;
import com.rentalert.listing_ms.domain.HiddenListingEmbeddedId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HiddenListingRepository extends JpaRepository<HiddenListing, HiddenListingEmbeddedId> {

    boolean existsByIdUserIdAndIdListingId(Long userId, Long listingId);

    @EntityGraph(attributePaths = {"listing"})
    @Query("SELECT h FROM HiddenListing h WHERE h.id.userId = :userId")
    Page<HiddenListing> findAllByUserIdWithListing(@Param("userId") Long userId, Pageable pageable);
}
