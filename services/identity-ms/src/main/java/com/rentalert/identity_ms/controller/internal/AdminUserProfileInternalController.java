package com.rentalert.identity_ms.controller.internal;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.profile.UpdateUserRoleRequest;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.identity_ms.config.PublicUrls;
import com.rentalert.identity_ms.service.AdminUserProfileService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG)
@RequiredArgsConstructor
public class AdminUserProfileInternalController {

    private final AdminUserProfileService adminUserProfileService;

    @GetMapping("/profiles")
    public PageResponse<UserProfileResponse> findAll(
        @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestParam(required = false) String filter
    ) {
        return PageUtil.toResponse(adminUserProfileService.findAll(pageable, filter));
    }

    @GetMapping("/profiles/{userId}")
    public UserProfileResponse findById(@PathVariable Long userId) {
        return adminUserProfileService.findById(userId);
    }

    @PutMapping("/profiles/{userId}/role")
    public UserProfileResponse updateRole(@PathVariable Long userId, @Valid @RequestBody UpdateUserRoleRequest request) {
        return adminUserProfileService.updateRole(userId, request.role());
    }

    @DeleteMapping("/profiles/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        adminUserProfileService.delete(userId);
    }

    @PostMapping("/profiles/briefs")
    public List<UserBriefResponse> findBriefs(@RequestBody List<Long> userIds) {
        return adminUserProfileService.findBriefs(userIds);
    }

    @PostMapping("/broadcast-targets")
    public List<BroadcastTarget> findBroadcastTargets(@RequestBody List<Long> userIds) {
        return adminUserProfileService.findBroadcastTargets(userIds);
    }
}
