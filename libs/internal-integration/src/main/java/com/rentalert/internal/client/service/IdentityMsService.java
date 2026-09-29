package com.rentalert.internal.client.service;

import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import java.util.Optional;

public interface IdentityMsService {

    UserProfileResponse getProfile(Long userId);

    Optional<UserFilterResponse> getActiveFilter(Long userId);
}
