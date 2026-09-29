package com.rentalert.internal.client.service;

import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.internal.client.AbstractResilientClient;
import com.rentalert.internal.client.IdentityMsClient;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.Optional;

public class IdentityMsServiceImpl extends AbstractResilientClient implements IdentityMsService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final IdentityMsClient identityMsClient;
    private final CacheService cacheService;

    public IdentityMsServiceImpl(IdentityMsClient identityMsClient, CacheService cacheService,
                                 RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        super(IdentityMsClient.SERVICE_NAME, retryRegistry, circuitBreakerRegistry);
        this.identityMsClient = identityMsClient;
        this.cacheService = cacheService;
    }

    @Override
    public UserProfileResponse getProfile(Long userId) {
        Optional<UserProfileResponse> cached = cacheService.get(CacheType.USER_PROFILE_INTERNAL, userId);
        if (cached.isPresent()) {
            return cached.get();
        }
        UserProfileResponse profile = execute(() -> identityMsClient.getProfile(userId));
        cacheService.put(CacheType.USER_PROFILE_INTERNAL, userId, profile, CACHE_TTL);
        return profile;
    }

    @Override
    public Optional<UserFilterResponse> getActiveFilter(Long userId) {
        Optional<UserFilterResponse> cached = cacheService.get(CacheType.USER_ACTIVE_FILTER, userId);
        if (cached.isPresent()) {
            return cached;
        }
        UserFilterResponse filter = execute(() -> identityMsClient.getActiveFilter(userId));
        if (filter != null) {
            cacheService.put(CacheType.USER_ACTIVE_FILTER, userId, filter, CACHE_TTL);
        }
        return Optional.ofNullable(filter);
    }
}
