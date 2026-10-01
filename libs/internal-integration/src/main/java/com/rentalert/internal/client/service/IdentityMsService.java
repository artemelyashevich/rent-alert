package com.rentalert.internal.client.service;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.auth.AdminLoginRequest;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.profile.FeedbackResponse;
import com.rentalert.dto.profile.UpdateUserRoleRequest;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface IdentityMsService {

    UserProfileResponse getProfile(Long userId);

    NotificationRequest.Receiver getReceiver(Long userId);

    Optional<UserFilterResponse> getActiveFilter(Long userId);

    List<BroadcastTarget> findBroadcastTargets(List<Long> userIds);

    LoginResponse adminLogin(AdminLoginRequest request);

    PageResponse<FeedbackResponse> findFeedbacks(Pageable pageable);

    PageResponse<UserProfileResponse> findProfiles(String filter, Pageable pageable);

    UserProfileResponse findProfile(Long userId);

    UserProfileResponse updateRole(Long userId, UpdateUserRoleRequest request);

    void deleteProfile(Long userId);

    List<UserBriefResponse> findBriefs(Collection<Long> userIds);
}
