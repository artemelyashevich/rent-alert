package com.rentalert.roommate_ms.controller;

import com.rentalert.redis.service.UserSessionService;
import com.rentalert.roommate_ms.config.PublicUrls;
import com.rentalert.roommate_ms.dto.RoommateProfileRequest;
import com.rentalert.roommate_ms.dto.RoommateProfileResponse;
import com.rentalert.roommate_ms.dto.RoommateVisibilityRequest;
import com.rentalert.roommate_ms.service.RoommateProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Roommate profile", description = "Анкета соседа")
@RestController
@RequestMapping(PublicUrls.PROFILE_TAG)
@RequiredArgsConstructor
public class RoommateProfileController {

    private final RoommateProfileService profileService;
    private final UserSessionService userSessionService;

    @Operation(summary = "Своя анкета", description = "404, если анкета ещё не заполнена.")
    @GetMapping("/me")
    public ResponseEntity<RoommateProfileResponse> getMine() {
        return ResponseEntity.of(profileService.findMine(currentUserId()));
    }

    @Operation(summary = "Создать/обновить анкету (upsert)")
    @PutMapping
    public RoommateProfileResponse upsert(@Valid @RequestBody RoommateProfileRequest request) {
        return profileService.upsert(currentUserId(), request);
    }

    @Operation(summary = "Пауза/возобновление показа в ленте")
    @PatchMapping("/visibility")
    public RoommateProfileResponse setVisibility(@Valid @RequestBody RoommateVisibilityRequest request) {
        return profileService.setSearchable(currentUserId(), request.searchable());
    }

    @Operation(summary = "Выйти из раздела «Соседи»", description = "Мягкое отключение: анкета не удаляется.")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate() {
        profileService.deactivate(currentUserId());
    }

    private Long currentUserId() {
        return userSessionService.getCurrentUser().getUserId();
    }
}
