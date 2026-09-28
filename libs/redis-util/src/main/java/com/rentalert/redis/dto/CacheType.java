package com.rentalert.redis.dto;

import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.user.UserSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;

/**
 * A cache and the type of its values. {@link #name()} is the Redis key prefix: {@code USER_SESSION:<token>}.
 */
@Getter
@RequiredArgsConstructor
public enum CacheType {

    USER_PROFILE(new TypeReference<UserProfileResponse>() {}),
    USER_SESSION(new TypeReference<UserSession>() {}),
    USER_ACTIVE_TOKEN(new TypeReference<String>() {});

    private final TypeReference<?> type;
}
