package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteCreateDto;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteResponse;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteUpdateDto;
import java.util.List;

public interface RoommateSharedFavoriteService {

    List<RoommateSharedFavoriteResponse> findAll(Long matchId, Long requesterUserId);

    RoommateSharedFavoriteResponse add(Long matchId, Long requesterUserId, RoommateSharedFavoriteCreateDto request);

    RoommateSharedFavoriteResponse updateStatus(
        Long matchId, Long requesterUserId, Long listingId, RoommateSharedFavoriteUpdateDto request);

    void remove(Long matchId, Long requesterUserId, Long listingId);
}
