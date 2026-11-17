package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.util.jpa.SpecificationUtil;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.UserRole;
import com.rentalert.identity_ms.domain.DetailedUserProfileView;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.mapper.DetailedUserProfileMapper;
import com.rentalert.identity_ms.mapper.UserProfileMapper;
import com.rentalert.identity_ms.messaging.UserEventPublisher;
import com.rentalert.identity_ms.repository.DetailedUserProfileViewRepository;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.AdminUserProfileService;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.redis.service.UserSessionService;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserProfileServiceImpl implements AdminUserProfileService {

    private static final String NOT_FOUND_MESSAGE = "Профиль пользователя не найден";

    private final UserProfileRepository userProfileRepository;
    private final DetailedUserProfileViewRepository detailedUserProfileViewRepository;
    private final DetailedUserProfileMapper detailedUserProfileMapper;
    private final UserEventPublisher userEventPublisher;
    private final UserProfileMapper userProfileMapper;
    private final UserProfileService userProfileService;
    private final UserSessionService userSessionService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> findAll(Pageable pageable, String filter) {
        Specification<DetailedUserProfileView> specification = SpecificationUtil.buildSearchSpecification(
            filter,
            List.of(DetailedUserProfileView.Fields.username, DetailedUserProfileView.Fields.email),
            DetailedUserProfileView.Fields.id
        );
        return detailedUserProfileViewRepository.findAll(specification, pageable)
            .map(detailedUserProfileMapper::toUserProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse findById(Long id) {
        return detailedUserProfileViewRepository.findById(id)
            .map(detailedUserProfileMapper::toUserProfileResponse)
            .orElseThrow(() -> new AppException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional
    public UserProfileResponse updateRole(Long id, UserRole role) {
        UserProfile profile = userProfileRepository.findById(id)
            .orElseThrow(() -> new AppException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        profile.getSettings().setRoleName(role);
        UserProfile saved = userProfileRepository.save(profile);
        userProfileService.evictCachedProfile(id);
        invalidateSessionAfterCommit(id);
        log.info("Role of user {} changed to {}", id, role);
        return userProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userProfileRepository.existsById(id)) {
            throw new AppException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND);
        }
        userProfileRepository.deleteById(id);
        userEventPublisher.publish(UserEvent.USER_DELETED, id);

        userProfileService.evictCachedProfile(id);
        invalidateSessionAfterCommit(id);
        log.info("User profile {} deleted, other services are notified", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBriefResponse> findBriefs(Collection<Long> ids) {
        return userProfileRepository.findAllById(ids).stream()
            .map(profile -> new UserBriefResponse(profile.getId(), profile.getUsername(), profile.getPhotoUrl()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BroadcastTarget> findBroadcastTargets(List<Long> userIds) {
        List<UserProfile> profiles = userIds == null || userIds.isEmpty()
            ? userProfileRepository.findAllNonAdmins()
            : userProfileRepository.findNonAdminsByIdIn(userIds);
        return profiles.stream()
            .map(profile -> new BroadcastTarget(profile.getId(), profile.getTelegramId(), profile.getSettings().getBotStatus()))
            .toList();
    }

    private void invalidateSessionAfterCommit(Long userId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                userSessionService.invalidateSession(userId);
            }
        });
    }
}
