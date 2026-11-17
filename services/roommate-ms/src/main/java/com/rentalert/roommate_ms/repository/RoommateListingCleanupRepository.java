package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateListing;
import java.util.Collection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface RoommateListingCleanupRepository extends Repository<RoommateListing, Long> {

    @Modifying
    @Query(value = "DELETE FROM roommate_shared_favorites WHERE listing_id IN (:listingIds)", nativeQuery = true)
    int deleteSharedFavoritesByListingIdIn(@Param("listingIds") Collection<Long> listingIds);

    @Modifying
    @Query(value = "UPDATE roommate_listings SET listing_id = NULL WHERE listing_id IN (:listingIds)", nativeQuery = true)
    int clearListingIdInListings(@Param("listingIds") Collection<Long> listingIds);

    @Modifying
    @Query(value = "UPDATE roommate_viewings SET listing_id = NULL WHERE listing_id IN (:listingIds)", nativeQuery = true)
    int clearListingIdInViewings(@Param("listingIds") Collection<Long> listingIds);
}
