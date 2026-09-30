package com.rentalert.identity_ms.service;

import com.rentalert.dto.profile.TelegramUser;
import com.rentalert.dto.profile.UserProfileCreateDto;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.UserProfileUpdateDto;
import com.rentalert.identity_ms.domain.UserProfile;

public interface UserProfileService {

    UserProfile create(UserProfileCreateDto createDto);

    UserProfile update(Long id, UserProfileUpdateDto updateDto);

    UserProfile update(UserProfileUpdateDto userProfileUpdateDto);

    UserProfile getById(Long id);

    UserProfile getByTelegramId(String telegramId);

    UserProfile findOrRegister(TelegramUser telegramUser, String telegramId);

    String encryptRefreshToken(String refreshToken);

    String getDecryptedRefreshToken(UserProfile profile);

    void save(UserProfile profile);

    UserProfileResponse findCurrentUser();

    void evictCachedProfile(Long userId);
}
