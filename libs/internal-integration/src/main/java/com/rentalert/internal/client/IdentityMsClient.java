package com.rentalert.internal.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.auth.AdminLoginRequest;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.notification.BroadcastTarget;
import com.rentalert.dto.notification.NotificationDelivery;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.profile.FeedbackResponse;
import com.rentalert.dto.profile.TelegramIdRequest;
import com.rentalert.dto.profile.UpdateUserRoleRequest;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
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

@FeignClient(name = IdentityMsClient.SERVICE_NAME, path = "/identity/internal")
public interface IdentityMsClient {

    String SERVICE_NAME = "identity-ms";

    @GetMapping("/profiles/{userId}")
    UserProfileResponse getProfile(@PathVariable("userId") Long userId);

    @GetMapping("/profiles/{userId}/receiver")
    NotificationRequest.Receiver getReceiver(@PathVariable("userId") Long userId);

    @GetMapping("/profiles/{userId}/delivery")
    NotificationDelivery getDelivery(@PathVariable("userId") Long userId);

    @PostMapping("/profiles/briefs")
    List<UserBriefResponse> findUserBriefs(@RequestBody Collection<Long> userIds);

    @GetMapping("/profiles/search")
    List<UserBriefResponse> searchUserBriefs(@RequestParam("username") String username);

    @PostMapping("/profiles/by-telegram")
    UserProfileResponse findByTelegramId(@RequestBody TelegramIdRequest request);

    @GetMapping("/filters/active/{userId}")
    UserFilterResponse getActiveFilter(@PathVariable("userId") Long userId);

    @GetMapping("/filters/match/{matchId}")
    UserFilterResponse getMatchFilter(@PathVariable("matchId") Long matchId);

    @PutMapping("/filters/match/{matchId}")
    UserFilterResponse upsertMatchFilter(@PathVariable("matchId") Long matchId, @RequestBody UserFilterRequest request);

    @PutMapping("/filters/match/{matchId}/activation/{active}")
    UserFilterResponse setMatchFilterActivation(
        @PathVariable("matchId") Long matchId,
        @PathVariable("active") boolean active
    );

    @PostMapping("/admin/login")
    LoginResponse adminLogin(@RequestBody AdminLoginRequest request);

    @GetMapping("/admin/feedbacks")
    PageResponse<FeedbackResponse> findFeedbacks(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @GetMapping("/admin/profiles")
    PageResponse<UserProfileResponse> findProfiles(
        @RequestParam(value = "filter", required = false) String filter,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @GetMapping("/admin/profiles/{userId}")
    UserProfileResponse findProfile(@PathVariable("userId") Long userId);

    @PutMapping("/admin/profiles/{userId}/role")
    UserProfileResponse updateRole(@PathVariable("userId") Long userId, @RequestBody UpdateUserRoleRequest request);

    @DeleteMapping("/admin/profiles/{userId}")
    void deleteProfile(@PathVariable("userId") Long userId);

    @PostMapping("/admin/profiles/briefs")
    List<UserBriefResponse> findBriefs(@RequestBody Collection<Long> userIds);

    @PostMapping("/admin/broadcast-targets")
    List<BroadcastTarget> findBroadcastTargets(@RequestBody List<Long> userIds);
}
