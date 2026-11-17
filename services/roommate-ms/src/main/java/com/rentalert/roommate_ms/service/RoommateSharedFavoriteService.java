package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteCreateDto;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteResponse;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteUpdateDto;
import java.util.List;

public interface RoommateSharedFavoriteService {

    /**
     * Shared favorites of the match with listing cards fetched in one batch; cards are null if listing-ms is unavailable.
     */
    List<RoommateSharedFavoriteResponse> findAll(Long matchId, Long requesterUserId);

    /**
     * Adds a listing to the shared favorites; 400 if it is already there, 404 if the listing does not exist.
     */
    RoommateSharedFavoriteResponse add(Long matchId, Long requesterUserId, RoommateSharedFavoriteCreateDto request);

    /**
     * Changes the status and viewing date of a shared favorite; 404 if the listing is not in the favorites.
     */
    RoommateSharedFavoriteResponse updateStatus(
        Long matchId, Long requesterUserId, Long listingId, RoommateSharedFavoriteUpdateDto request);

    void remove(Long matchId, Long requesterUserId, Long listingId);
}
