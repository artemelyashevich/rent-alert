package com.rentalert.identity_ms.service;

import com.rentalert.dto.notification.NotificationDelivery;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.profile.TelegramUser;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileCreateDto;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.UserProfileUpdateDto;
import com.rentalert.identity_ms.domain.UserProfile;
import java.util.Collection;
import java.util.List;

public interface UserProfileService {

    UserProfile create(UserProfileCreateDto createDto);

    UserProfile update(Long id, UserProfileUpdateDto updateDto);

    UserProfile update(UserProfileUpdateDto userProfileUpdateDto);

    UserProfile getById(Long id);

    NotificationRequest.Receiver getReceiver(Long id);

    NotificationDelivery getDelivery(Long id);

    List<UserBriefResponse> findBriefs(Collection<Long> ids);

    List<UserBriefResponse> searchBriefsByUsername(String username);

    UserProfile getByTelegramId(String telegramId);

    UserProfile findOrRegister(TelegramUser telegramUser, String telegramId);

    String encryptRefreshToken(String refreshToken);

    String getDecryptedRefreshToken(UserProfile profile);

    void save(UserProfile profile);

    UserProfileResponse findCurrentUser();

    void evictCachedProfile(Long userId);
}
