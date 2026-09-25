package com.rentalert.identity_ms.service;

import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.dto.profile.UserProfileCreateDto;
import com.rentalert.identity_ms.dto.profile.UserProfileUpdateDto;

public interface UserProfileService {
    UserProfile create(UserProfileCreateDto createDto);

    UserProfile update(Long id, UserProfileUpdateDto updateDto);

    UserProfile update(UserProfileUpdateDto userProfileUpdateDto);

    UserProfile getById(Long id);

}
