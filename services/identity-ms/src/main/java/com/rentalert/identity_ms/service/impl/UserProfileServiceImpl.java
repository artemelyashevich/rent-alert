package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.auth.CryptoService;
import com.rentalert.common.auth.HmacBlindIndexService;
import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.AppNotFoundException;
import com.rentalert.dto.user.UserSession;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.identity_ms.dto.profile.UserProfileCreateDto;
import com.rentalert.identity_ms.dto.profile.UserProfileUpdateDto;
import com.rentalert.identity_ms.mapper.UserProfileMapper;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.redis.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String CACHE_NAME = "user-profiles";

    private final UserProfileRepository userProfileRepository;
    private final CacheManager cacheManager;
    private final CryptoService cryptoService;
    private final UserProfileMapper userProfileMapper;
    private final HmacBlindIndexService blindIndexService;
    private final UserSessionService userSessionService;
    private final TransactionTemplate transactionTemplate;

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
    @Cacheable(value = CACHE_NAME, key = "#id")
    public UserProfile getById(Long id) {
        log.debug("Fetching user profile with ID: {}", id);
        return userProfileRepository.findById(id).orElseThrow(AppNotFoundException::new);
    }

    private void evictCachedProfile(Long id) {
        if (id == null) {
            return;
        }
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evictIfPresent(id);
            log.debug("Evicted cached user profile with id: {}", id);
        }
    }
}