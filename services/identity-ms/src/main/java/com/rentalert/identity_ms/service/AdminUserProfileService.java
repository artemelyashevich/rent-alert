package com.rentalert.identity_ms.service;

import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserProfileService {

    Page<UserProfileResponse> findAll(Pageable pageable, String filter);

    UserProfileResponse findById(Long id);

    UserProfileResponse updateRole(Long id, UserRole role);

    void delete(Long id);

    List<UserBriefResponse> findBriefs(Collection<Long> ids);

    List<BroadcastTarget> findBroadcastTargets(List<Long> userIds);
}
