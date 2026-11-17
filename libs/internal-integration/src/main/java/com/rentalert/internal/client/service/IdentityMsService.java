package com.rentalert.internal.client.service;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.auth.AdminLoginRequest;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.notification.NotificationDelivery;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.profile.FeedbackResponse;
import com.rentalert.dto.profile.UpdateUserRoleRequest;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface IdentityMsService {

    UserProfileResponse getProfile(Long userId);

    NotificationRequest.Receiver getReceiver(Long userId);

    /**
     * The user's encrypted addresses and the end of their quiet hours, for notifications sent by services that
     * don't own the user's settings.
     */
    NotificationDelivery getDelivery(Long userId);

    Optional<UserProfileResponse> findByTelegramId(String telegramId);

    Optional<UserFilterResponse> getActiveFilter(Long userId);

    /** The shared filter of a roommate match, empty if it has not been created yet. */
    Optional<UserFilterResponse> getMatchFilter(Long matchId);

    /** Creates or updates the shared filter of a roommate match. */
    UserFilterResponse upsertMatchFilter(Long matchId, UserFilterRequest request);

    /** Turns the shared filter of a roommate match on or off. */
    UserFilterResponse setMatchFilterActivation(Long matchId, boolean active);

    List<BroadcastTarget> findBroadcastTargets(List<Long> userIds);

    LoginResponse adminLogin(AdminLoginRequest request);

    PageResponse<FeedbackResponse> findFeedbacks(Pageable pageable);

    PageResponse<UserProfileResponse> findProfiles(String filter, Pageable pageable);

    UserProfileResponse findProfile(Long userId);

    UserProfileResponse updateRole(Long userId, UpdateUserRoleRequest request);

    void deleteProfile(Long userId);

    List<UserBriefResponse> findBriefs(Collection<Long> userIds);

    /**
     * Name and photo of the given users for user-facing screens; unlike {@link #findBriefs} it needs no admin
     * token. An empty input makes no call.
     */
    List<UserBriefResponse> findUserBriefs(Collection<Long> userIds);
}
