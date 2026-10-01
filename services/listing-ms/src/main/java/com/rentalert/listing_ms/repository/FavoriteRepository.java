package com.rentalert.listing_ms.repository;

import com.rentalert.listing_ms.domain.Favorite;
import com.rentalert.listing_ms.domain.FavoriteEmbeddedId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteEmbeddedId>,
    JpaSpecificationExecutor<Favorite> {

    int countByIdUserId(Long userId);

    @EntityGraph(attributePaths = {"listing"})
    List<Favorite> findAllByIdIn(List<FavoriteEmbeddedId> ids);

    boolean existsByIdUserIdAndIdListingId(Long userId, Long listingId);

    @Modifying
    @Query("delete from Favorite f where f.id.listingId in :listingIds")
    int deleteByListingIdIn(@Param("listingIds") List<Long> listingIds);

    @Modifying
    @Query("delete from Favorite f where f.id.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
