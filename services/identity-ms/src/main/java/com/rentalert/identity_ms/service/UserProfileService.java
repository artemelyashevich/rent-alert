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

    /**
     * The user's addresses together with the moment their quiet hours end, so a producer outside identity-ms can
     * schedule a notification the way identity-ms does for its own.
     */
    NotificationDelivery getDelivery(Long id);

    /**
     * Public name and photo of the given users for other services' UI; unknown ids are skipped.
     */
    List<UserBriefResponse> findBriefs(Collection<Long> ids);

    UserProfile getByTelegramId(String telegramId);

    UserProfile findOrRegister(TelegramUser telegramUser, String telegramId);

    String encryptRefreshToken(String refreshToken);

    String getDecryptedRefreshToken(UserProfile profile);

    void save(UserProfile profile);

    UserProfileResponse findCurrentUser();

    void evictCachedProfile(Long userId);
}
