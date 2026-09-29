package com.rentalert.identity_ms.controller.internal;

import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.identity_ms.config.PublicUrls;
import com.rentalert.identity_ms.mapper.UserProfileMapper;
import com.rentalert.identity_ms.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
