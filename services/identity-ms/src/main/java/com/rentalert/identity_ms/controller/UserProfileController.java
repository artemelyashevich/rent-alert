package com.rentalert.identity_ms.controller;

import com.rentalert.identity_ms.dto.profile.UserProfileResponse;
import com.rentalert.identity_ms.dto.profile.UserProfileUpdateDto;
import com.rentalert.identity_ms.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser() {
        return userProfileService.findCurrentUser();
    }

    @PutMapping
    public UserProfileResponse update(@Valid @RequestBody UserProfileUpdateDto userProfileUpdateDto) {
        return userProfileService.update(userProfileUpdateDto);
    }
}
