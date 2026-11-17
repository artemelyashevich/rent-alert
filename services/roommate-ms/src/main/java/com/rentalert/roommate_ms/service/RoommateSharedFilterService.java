package com.rentalert.roommate_ms.service;

import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import java.util.Optional;

public interface RoommateSharedFilterService {

    /**
     * The shared filter of the match, empty if it has not been created yet.
     */
    Optional<UserFilterResponse> find(Long matchId, Long requesterUserId);

    /**
     * Creates or updates the shared filter of the match.
     */
    UserFilterResponse upsert(Long matchId, Long requesterUserId, UserFilterRequest request);

    UserFilterResponse setActivation(Long matchId, Long requesterUserId, boolean active);
}
