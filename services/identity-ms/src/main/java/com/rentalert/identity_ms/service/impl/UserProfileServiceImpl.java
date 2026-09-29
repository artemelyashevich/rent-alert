package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.common.util.auth.HmacBlindIndexService;
import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.AppNotFoundException;
import com.rentalert.dto.user.UserSession;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.dto.profile.TelegramUser;
import com.rentalert.dto.profile.UserProfileCreateDto;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.UserProfileUpdateDto;
import com.rentalert.identity_ms.mapper.DetailedUserProfileMapper;
import com.rentalert.identity_ms.mapper.UserProfileMapper;
import com.rentalert.identity_ms.repository.DetailedUserProfileViewRepository;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import com.rentalert.redis.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final DetailedUserProfileViewRepository detailedUserProfileViewRepository;
    private final DetailedUserProfileMapper detailedUserProfileMapper;
    private final CacheService cacheService;
    private final CryptoService cryptoService;
    private final UserProfileMapper userProfileMapper;
    private final HmacBlindIndexService blindIndexService;
    private final UserSessionService userSessionService;
    private final TransactionTemplate transactionTemplate;
    private final ProfileCacheEvictor profileCacheEvictor;

    @Override
    @Transactional
    public UserProfile create(UserProfileCreateDto createDto) {
        log.debug("Attempting create user profile for username: {}", createDto.getUsername());
        UserProfile entity = userProfileMapper.toEntity(createDto);
        entity.setTelegramId(cryptoService.encrypt(createDto.getTelegramId()));
        entity.setTelegramIdHash(blindIndexService.hash(createDto.getTelegramId()));
        entity.setSettings(UserSettings.builder()
            .botStatus(createDto.getBotStatus())
            .quietFrom(createDto.getQuietFrom())
            .quietTo(createDto.getQuietTo())
            .build());
        UserProfile saved = userProfileRepository.save(entity);
        log.debug("User profile has been created with id: {}", saved.getId());
        return saved;
    }

    @Override
    public UserProfile update(Long id, UserProfileUpdateDto updateDto) {
        UserProfile response = transactionTemplate.execute(t -> {
            log.debug("Attempting update user profile with ID: {}", id);

            UserProfile existingEntity = userProfileRepository.findById(id)
                .orElseThrow(() -> new AppException("Профиль пользователя не найден", HttpStatus.NOT_FOUND));

            userProfileMapper.updateEntityFromDto(updateDto, existingEntity);

            UserProfile updated = userProfileRepository.save(existingEntity);

            log.debug("User profile has been updated with id: {}", id);
            return updated;
        });
        evictCachedProfile(id);
        return response;
    }

    @Override
    public UserProfile update(UserProfileUpdateDto userProfileUpdateDto) {
        UserSession currentUser = userSessionService.getCurrentUser();
        return update(currentUser.getUserId(), userProfileUpdateDto);
    }

    @Override
    public UserProfile getById(Long id) {
        log.debug("Fetching user profile with ID: {}", id);
        return userProfileRepository.findById(id).orElseThrow(AppNotFoundException::new);
    }

    @Override
    public UserProfile getByTelegramId(String telegramId) {
        log.debug("Fetching user profile by telegramId");

        String hash = blindIndexService.hash(telegramId);
        return userProfileRepository.findByTelegramIdHash(hash).orElseThrow(() -> new AppException("Профиль пользователя не найден", HttpStatus.NOT_FOUND));
    }


    @Override
    @Transactional
    public UserProfile findOrRegister(TelegramUser telegramUser, String telegramId) {
        String hash = blindIndexService.hash(telegramId);
        return userProfileRepository.findByTelegramIdHash(hash)
            .orElseGet(() -> {
                registerInNewTransaction(telegramUser);
                return userProfileRepository.findByTelegramIdHash(hash).orElseThrow(AppNotFoundException::new);
            });
    }

    @Override
    public String encryptRefreshToken(String refreshToken) {
        return cryptoService.encrypt(refreshToken);
    }

    @Override
    public String getDecryptedRefreshToken(UserProfile profile) {
        return profile.getSettings().getRefreshTokenHash() == null
            ? null
            : cryptoService.decrypt(profile.getSettings().getRefreshTokenHash());
    }

    @Override
    public void save(UserProfile profile) {
        userProfileRepository.save(profile);
        evictCachedProfile(profile.getId());
    }

    @Override
    public UserProfileResponse findCurrentUser() {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return cacheService.get(CacheType.USER_PROFILE, userId, () -> detailedUserProfileViewRepository.findById(userId)
            .map(detailedUserProfileMapper::toUserProfileResponse)
            .orElseThrow(AppNotFoundException::new));
    }

    private void registerInNewTransaction(TelegramUser telegramUser) {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionTemplate.getTransactionManager());
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            requiresNew.executeWithoutResult(status -> register(telegramUser));
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent registration detected for telegram user, using the existing profile");
        }
    }

    private UserProfile register(TelegramUser telegramUser) {
        String telegramId = telegramUser.getId().toString();
        UserProfileCreateDto createDto = UserProfileCreateDto.builder()
            .telegramId(telegramId)
            .username(telegramUser.getUsername())
            .photoUrl(telegramUser.getPhotoUrl())
            .build();
         return create(createDto);
    }

     @Override
    public void evictCachedProfile(Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            profileCacheEvictor.evict(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                profileCacheEvictor.evict(userId);
            }
        });
    }
}