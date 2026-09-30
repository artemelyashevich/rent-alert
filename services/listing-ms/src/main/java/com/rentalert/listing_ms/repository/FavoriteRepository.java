package com.rentalert.listing_ms.repository;

import com.rentalert.listing_ms.domain.Favorite;
import com.rentalert.listing_ms.domain.FavoriteEmbeddedId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteEmbeddedId>,
    JpaSpecificationExecutor<Favorite> {

    int countByIdUserId(Long userId);

    @EntityGraph(attributePaths = {"listing"})
    List<Favorite> findAllByIdIn(List<FavoriteEmbeddedId> ids);

    boolean existsByIdUserIdAndIdListingId(Long userId, Long listingId);
}
