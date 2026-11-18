package com.rentalert.identity_ms.controller.internal;

import com.rentalert.dto.notification.NotificationDelivery;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.profile.TelegramIdRequest;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.identity_ms.config.PublicUrls;
import com.rentalert.identity_ms.mapper.UserProfileMapper;
import com.rentalert.identity_ms.service.UserProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_PROFILE_TAG)
@RequiredArgsConstructor
public class UserProfileInternalController {

    private final UserProfileMapper userProfileMapper;
    private final UserProfileService userProfileService;

    @GetMapping("/{userId}")
    public UserProfileResponse findByName(@PathVariable Long userId) {
        return userProfileMapper.toResponse(userProfileService.getById(userId));
    }

    @GetMapping("/{userId}/receiver")
    public NotificationRequest.Receiver findReceiver(@PathVariable Long userId) {
        return userProfileService.getReceiver(userId);
    }

    @GetMapping("/{userId}/delivery")
    public NotificationDelivery findDelivery(@PathVariable Long userId) {
        return userProfileService.getDelivery(userId);
    }

    @PostMapping("/briefs")
    public List<UserBriefResponse> findBriefs(@RequestBody List<Long> userIds) {
        return userProfileService.findBriefs(userIds);
    }

    @GetMapping("/search")
    public List<UserBriefResponse> searchByUsername(@RequestParam("username") String username) {
        return userProfileService.searchBriefsByUsername(username);
    }

    @PostMapping("/by-telegram")
    public UserProfileResponse findByTelegramId(@RequestBody TelegramIdRequest request) {
        return userProfileMapper.toResponse(userProfileService.getByTelegramId(request.telegramId()));
    }
}
