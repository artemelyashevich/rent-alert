package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.mapper.UserFilterMapper;
import com.rentalert.identity_ms.messaging.UserEventPublisher;
import com.rentalert.identity_ms.repository.UserFilterRepository;
import com.rentalert.identity_ms.service.UserFilterService;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.redis.service.UserSessionService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFilterServiceImpl implements UserFilterService {

    private static final int MAX_FILTERS_PER_USER = 5;

    private final UserFilterRepository userFilterRepository;
    private final UserProfileService userProfileService;
    private final UserFilterMapper userFilterMapper;
    private final UserSessionService userSessionService;
    private final UserEventPublisher userEventPublisher;

    @Override
    @Transactional
    public UserFilterResponse create(UserFilterRequest request) {
        Long userProfileId = userSessionService.getCurrentUser().getUserId();
        log.debug("Attempting to create filter for userProfileId={}", userProfileId);

        int count = userFilterRepository.countByUserProfileId(userProfileId);
        if (count >= MAX_FILTERS_PER_USER) {
            String message = "User profile %d already has %d filters (maximum allowed)".formatted(userProfileId, MAX_FILTERS_PER_USER);
            log.warn(message);
            throw new AppException(message, HttpStatus.BAD_REQUEST);
        }

        UserProfile userProfile = userProfileService.getById(userProfileId);

        UserFilter filter = userFilterMapper.toEntity(request);
        filter.setUserProfile(userProfile);

        UserFilter saved = userFilterRepository.save(filter);
        log.info("Filter has been created: id={}, userProfileId={}", saved.getId(), userProfileId);
        userEventPublisher.publish(UserEvent.FILTER_CREATED, userProfileId, saved.getId());
        userProfileService.evictCachedProfile(userProfileId);

        return userFilterMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserFilter createDefault(UserProfile userProfile) {
        log.debug("Creating default filter for userProfileId={}", userProfile.getId());

        UserFilter filter = UserFilter.builder()
            .userProfile(userProfile)
            .currency(Currency.USD)
            .city("Минск")
            .active(true)
            .build();

        UserFilter saved = userFilterRepository.save(filter);
        log.info("Default filter has been created: id={}, userProfileId={}", saved.getId(), userProfile.getId());
        userProfileService.evictCachedProfile(userProfile.getId());
        return saved;
    }

    @Override
    @Transactional
    public UserFilterResponse update(Long filterId, UserFilterRequest request) {
        Long userProfileId = userSessionService.getCurrentUser().getUserId();
        log.debug("Attempting to update filterId={} for userProfileId={}", filterId, userProfileId);

        UserFilter filter = getFilter(userProfileId, filterId);
        userFilterMapper.update(filter, request);

        UserFilter saved = userFilterRepository.save(filter);
        log.info("Filter has been updated: id={}, userProfileId={}", filterId, userProfileId);
        userEventPublisher.publish(UserEvent.FILTER_UPDATED, userProfileId, filterId);
        userProfileService.evictCachedProfile(userProfileId);

        return userFilterMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserFilterResponse setActivation(Long filterId, boolean active) {
        Long userProfileId = userSessionService.getCurrentUser().getUserId();
        log.debug("Attempting to set activation={} for filterId={} for userProfileId={}", active, filterId, userProfileId);
        if (active) {
            userFilterRepository.findByUserProfileIdAndActiveTrue(userProfileId)
                .ifPresent(current -> {
                    current.setActive(false);
                    userFilterRepository.save(current);
                    log.debug("Deactivated filterId={} for userProfileId={}", current.getId(), userProfileId);
                });
        }
        UserFilter filter = getFilter(userProfileId, filterId);
        filter.setActive(active);
        UserFilter saved = userFilterRepository.save(filter);
        log.info("Filter activation has been set to {}: id={}, userProfileId={}", active, filterId, userProfileId);
        if (active) {
            userEventPublisher.publish(UserEvent.FILTER_ACTIVATED, userProfileId, filterId);
        }
        userProfileService.evictCachedProfile(userProfileId);
        return userFilterMapper.toResponse(saved);
    }

    @Override
    public UserFilterResponse findById(Long filterId) {
        Long userProfileId = userSessionService.getCurrentUser().getUserId();
        log.debug("Attempting to find filterId={} for userProfileId={}", filterId, userProfileId);
        UserFilterResponse response = userFilterMapper.toResponse(getFilter(userProfileId, filterId));
        log.info("Filter has been found: id={}, userProfileId={}", filterId, userProfileId);
        return response;
    }

    @Override
    public List<UserFilterResponse> findAll() {
        Long userProfileId = userSessionService.getCurrentUser().getUserId();
        log.debug("Attempting to find all filters for userProfileId={}", userProfileId);
        List<UserFilter> filters = userFilterRepository.findAllByUserProfileId(userProfileId);
        log.info("Found {} filters for userProfileId={}", filters.size(), userProfileId);
        return userFilterMapper.toResponse(filters);
    }

    @Override
    public List<UserFilterResponse> findAllActive() {
        log.debug("Attempting to findAll active filters");
        List<UserFilter> filters = userFilterRepository.findAllByActive(true);
        log.info("Found {} filters for active", filters.size());
        return userFilterMapper.toResponse(filters);
    }

    @Override
    @Transactional
    public void delete(Long filterId) {
        Long userProfileId = userSessionService.getCurrentUser().getUserId();
        log.debug("Attempting to delete filterId={} for userProfileId={}", filterId, userProfileId);
        UserFilter filter = getFilter(userProfileId, filterId);

        if (filter.isActive()) {
            String message = "Cannot delete active filter with id: %d. Deactivate it first".formatted(filterId);
            log.warn(message);
            throw new AppBadRequestException(message);
        }
        userFilterRepository.delete(filter);
        log.info("Filter has been deleted: id={}, userProfileId={}", filterId, userProfileId);
        userEventPublisher.publish(UserEvent.FILTER_DELETED, userProfileId, filterId);
        userProfileService.evictCachedProfile(userProfileId);
    }

    @Override
    public UserFilter findActiveFilterByUserId(Long userId) {
        log.debug("Attempting to find active filter for userId={}", userId);
        return userFilterRepository.findByUserProfileIdAndActiveTrue(userId)
            .orElse(null);
    }

    @Override
    public Optional<UserFilterResponse> findByOwnerMatchId(Long matchId) {
        return userFilterRepository.findByOwnerMatchId(matchId).map(userFilterMapper::toResponse);
    }

    @Override
    @Transactional
    public UserFilterResponse upsertForMatch(Long matchId, UserFilterRequest request) {
        UserFilter filter = userFilterRepository.findByOwnerMatchId(matchId).orElse(null);
        if (filter == null) {
            filter = userFilterMapper.toEntity(request);
            filter.setOwnerMatchId(matchId);
        } else {
            userFilterMapper.update(filter, request);
        }
        UserFilter saved = userFilterRepository.save(filter);
        log.info("Shared filter upserted for match {}", matchId);
        return userFilterMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserFilterResponse setActivationForMatch(Long matchId, boolean active) {
        UserFilter filter = userFilterRepository.findByOwnerMatchId(matchId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404, HttpStatus.NOT_FOUND));
        filter.setActive(active);
        return userFilterMapper.toResponse(userFilterRepository.save(filter));
    }

    private UserFilter getFilter(Long userProfileId, Long filterId) {
        return userFilterRepository.findByIdAndUserProfileId(filterId, userProfileId)
            .orElseThrow(() -> throwNotFoundException("UserFilter", filterId));
    }

    private AppException throwNotFoundException(String entity, Long id) {
        String message = "%s with id: %d not found".formatted(entity, id);
        log.warn(message);
        return new AppException(message, HttpStatus.NOT_FOUND);
    }
}
