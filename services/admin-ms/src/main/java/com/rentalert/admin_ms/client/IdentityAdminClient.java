package com.rentalert.admin_ms.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.auth.AdminLoginRequest;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.profile.FeedbackResponse;
import com.rentalert.dto.profile.UpdateUserRoleRequest;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import java.util.Collection;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "identity-ms", contextId = "identityAdminClient", path = "/identity/internal/admin")
public interface IdentityAdminClient {

    @PostMapping("/login")
    LoginResponse login(@RequestBody AdminLoginRequest request);

    @GetMapping("/feedbacks")
    PageResponse<FeedbackResponse> findFeedbacks(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @GetMapping("/profiles")
    PageResponse<UserProfileResponse> findProfiles(
        @RequestParam(value = "filter", required = false) String filter,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @GetMapping("/profiles/{userId}")
    UserProfileResponse findProfile(@PathVariable("userId") Long userId);

    @PutMapping("/profiles/{userId}/role")
    UserProfileResponse updateRole(@PathVariable("userId") Long userId, @RequestBody UpdateUserRoleRequest request);

    @DeleteMapping("/profiles/{userId}")
    void deleteProfile(@PathVariable("userId") Long userId);

    @PostMapping("/profiles/briefs")
    List<UserBriefResponse> findBriefs(@RequestBody Collection<Long> userIds);

    @PostMapping("/broadcast-targets")
    List<BroadcastTarget> findBroadcastTargets(@RequestBody List<Long> userIds);
}
