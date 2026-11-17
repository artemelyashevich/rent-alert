package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.RoommateProfileRequest;
import com.rentalert.roommate_ms.dto.RoommateProfileResponse;
import java.util.Optional;

public interface RoommateProfileService {

    Optional<RoommateProfileResponse> findMine(Long userId);

    /**
     * Creates or updates the user's profile. A new or changed bio goes through moderation; only an approved bio is
     * published, and every update of an existing profile bumps its version.
     */
    RoommateProfileResponse upsert(Long userId, RoommateProfileRequest request);

    RoommateProfileResponse setSearchable(Long userId, boolean searchable);

    /**
     * Leaves the roommate section: the profile is kept but hidden and inactive.
     */
    void deactivate(Long userId);
}
