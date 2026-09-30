package com.rentalert.listing_ms.repository;

import com.rentalert.listing_ms.domain.Listing;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<@NonNull Listing, @NonNull Long>,
    JpaSpecificationExecutor<Listing> {

    @Query("""
        select l.id from Listing l
        where l.parsedAt < :cutoff
          and not exists (
              select 1 from Favorite f
              where f.id.listingId = l.id and f.viewingDate > current_timestamp
          )
        order by l.id
        """)
    List<Long> findStaleListingIds(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying
    @Query("""
        delete from Listing l where l.id in :ids
        """)
    int deleteByIdIn(@Param("ids") List<Long> ids);
}
