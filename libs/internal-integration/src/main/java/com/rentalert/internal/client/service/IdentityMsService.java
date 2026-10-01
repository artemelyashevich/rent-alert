package com.rentalert.internal.client.service;

import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import java.util.List;
import java.util.Optional;

public interface IdentityMsService {

    UserProfileResponse getProfile(Long userId);

    NotificationRequest.Receiver getReceiver(Long userId);

    Optional<UserFilterResponse> getActiveFilter(Long userId);

    List<BroadcastTarget> findBroadcastTargets(List<Long> userIds);
}
