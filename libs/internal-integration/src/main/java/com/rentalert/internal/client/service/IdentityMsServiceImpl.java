package com.rentalert.internal.client.service;

import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.internal.client.AbstractResilientClient;
import com.rentalert.internal.client.IdentityMsClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

public class IdentityMsServiceImpl extends AbstractResilientClient implements IdentityMsService {

    private final IdentityMsClient identityMsClient;

    public IdentityMsServiceImpl(IdentityMsClient identityMsClient, RetryRegistry retryRegistry,
                                 CircuitBreakerRegistry circuitBreakerRegistry) {
        super(IdentityMsClient.SERVICE_NAME, retryRegistry, circuitBreakerRegistry);
        this.identityMsClient = identityMsClient;
    }

    @Override
    public UserProfileResponse getProfile(Long userId) {
        return execute(() -> identityMsClient.getProfile(userId));
    }
}
