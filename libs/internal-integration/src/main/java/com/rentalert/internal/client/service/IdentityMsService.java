package com.rentalert.internal.client.service;

import com.rentalert.dto.profile.UserProfileResponse;

public interface IdentityMsService {

    UserProfileResponse getProfile(Long userId);
}
