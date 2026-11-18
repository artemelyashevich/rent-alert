package com.rentalert.internal.client.service.impl;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.auth.AdminLoginRequest;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.notification.NotificationDelivery;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.profile.FeedbackResponse;
import com.rentalert.dto.profile.TelegramIdRequest;
import com.rentalert.dto.profile.UpdateUserRoleRequest;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.internal.client.AbstractResilientClient;
import com.rentalert.internal.client.IdentityMsClient;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

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
    public NotificationRequest.Receiver getReceiver(Long userId) {
        return execute(() -> identityMsClient.getReceiver(userId));
    }

    @Override
    public NotificationDelivery getDelivery(Long userId) {
        return execute(() -> identityMsClient.getDelivery(userId));
    }

    @Override
    public Optional<UserProfileResponse> findByTelegramId(String telegramId) {
        try {
            return Optional.of(execute(() -> identityMsClient.findByTelegramId(new TelegramIdRequest(telegramId))));
        } catch (UpstreamHttpException e) {
            if (e.getUpstreamStatus() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw e;
        }
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

    @Override
    public Optional<UserFilterResponse> getMatchFilter(Long matchId) {
        return Optional.ofNullable(execute(() -> identityMsClient.getMatchFilter(matchId)));
    }

    @Override
    public UserFilterResponse upsertMatchFilter(Long matchId, UserFilterRequest request) {
        return executeOnce(() -> identityMsClient.upsertMatchFilter(matchId, request));
    }

    @Override
    public UserFilterResponse setMatchFilterActivation(Long matchId, boolean active) {
        return executeOnce(() -> identityMsClient.setMatchFilterActivation(matchId, active));
    }

    @Override
    public List<BroadcastTarget> findBroadcastTargets(List<Long> userIds) {
        return execute(() -> identityMsClient.findBroadcastTargets(userIds));
    }

    @Override
    public LoginResponse adminLogin(AdminLoginRequest request) {
        return executeOnce(() -> identityMsClient.adminLogin(request));
    }

    @Override
    public PageResponse<FeedbackResponse> findFeedbacks(Pageable pageable) {
        return execute(() -> identityMsClient.findFeedbacks(pageable.getPageNumber(), pageable.getPageSize(),
            PageUtil.toSortParams(pageable)));
    }

    @Override
    public PageResponse<UserProfileResponse> findProfiles(String filter, Pageable pageable) {
        return execute(() -> identityMsClient.findProfiles(filter, pageable.getPageNumber(), pageable.getPageSize(),
            PageUtil.toSortParams(pageable)));
    }

    @Override
    public UserProfileResponse findProfile(Long userId) {
        return execute(() -> identityMsClient.findProfile(userId));
    }

    @Override
    public UserProfileResponse updateRole(Long userId, UpdateUserRoleRequest request) {
        return executeOnce(() -> identityMsClient.updateRole(userId, request));
    }

    @Override
    public void deleteProfile(Long userId) {
        executeOnce(() -> identityMsClient.deleteProfile(userId));
    }

    @Override
    public List<UserBriefResponse> findBriefs(Collection<Long> userIds) {
        return execute(() -> identityMsClient.findBriefs(userIds));
    }

    @Override
    public List<UserBriefResponse> findUserBriefs(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return execute(() -> identityMsClient.findUserBriefs(userIds));
    }

    @Override
    public List<UserBriefResponse> searchUserBriefs(String username) {
        return execute(() -> identityMsClient.searchUserBriefs(username));
    }
}
