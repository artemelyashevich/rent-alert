package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.RoommateProfileRequest;
import com.rentalert.roommate_ms.dto.RoommateProfileResponse;
import java.util.Optional;

public interface RoommateProfileService {

    Optional<RoommateProfileResponse> findMine(Long userId);

    RoommateProfileResponse upsert(Long userId, RoommateProfileRequest request);

    RoommateProfileResponse setSearchable(Long userId, boolean searchable);

    void deactivate(Long userId);
}
