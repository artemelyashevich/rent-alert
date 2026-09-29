package com.rentalert.listing_ms.repository;

import com.rentalert.listing_ms.domain.Favorite;
import com.rentalert.listing_ms.domain.FavoriteEmbeddedId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteEmbeddedId>,
                                            JpaSpecificationExecutor<Favorite> {

    int countByIdUserId(Long userId);

    @EntityGraph(attributePaths = {"listing"})
    List<Favorite> findAllByIdIn(List<FavoriteEmbeddedId> ids);

    boolean existsByIdUserIdAndIdListingId(Long userId, Long listingId);
}
