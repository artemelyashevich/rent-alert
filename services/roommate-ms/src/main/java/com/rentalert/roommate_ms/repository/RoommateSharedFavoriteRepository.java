package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateSharedFavorite;
import com.rentalert.roommate_ms.domain.RoommateSharedFavoriteEmbeddedId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoommateSharedFavoriteRepository
    extends JpaRepository<RoommateSharedFavorite, RoommateSharedFavoriteEmbeddedId> {

    List<RoommateSharedFavorite> findAllByIdMatchId(Long matchId);
}
