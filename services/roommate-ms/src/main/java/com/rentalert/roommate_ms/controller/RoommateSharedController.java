package com.rentalert.roommate_ms.controller;

import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.redis.service.UserSessionService;
import com.rentalert.roommate_ms.config.PublicUrls;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteCreateDto;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteResponse;
import com.rentalert.roommate_ms.dto.RoommateSharedFavoriteUpdateDto;
import com.rentalert.roommate_ms.service.RoommateSharedFavoriteService;
import com.rentalert.roommate_ms.service.RoommateSharedFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = PublicUrls.SHARED_MATCH_TAG_NAME, description = "Общий фильтр и общее избранное пары, открываются после контактов")
@RestController
@RequestMapping(PublicUrls.MATCH_TAG)
@RequiredArgsConstructor
public class RoommateSharedController {

    private final RoommateSharedFavoriteService sharedFavoriteService;
    private final RoommateSharedFilterService sharedFilterService;
    private final UserSessionService userSessionService;

    @Operation(summary = "Общий фильтр пары", description = "404, если ещё не создан.")
    @GetMapping("/{matchId}/filter")
    public ResponseEntity<UserFilterResponse> getSharedFilter(@PathVariable Long matchId) {
        return ResponseEntity.of(sharedFilterService.find(matchId, currentUserId()));
    }

    @Operation(summary = "Создать/обновить общий фильтр пары")
    @PutMapping("/{matchId}/filter")
    public UserFilterResponse upsertSharedFilter(@PathVariable Long matchId, @Valid @RequestBody UserFilterRequest request) {
        return sharedFilterService.upsert(matchId, currentUserId(), request);
    }

    @Operation(summary = "Включить/выключить общий фильтр пары")
    @PutMapping("/{matchId}/filter/activation/{active}")
    public UserFilterResponse setSharedFilterActivation(@PathVariable Long matchId, @PathVariable boolean active) {
        return sharedFilterService.setActivation(matchId, currentUserId(), active);
    }

    @Operation(summary = "Общее избранное пары")
    @GetMapping("/{matchId}/favorites")
    public List<RoommateSharedFavoriteResponse> getSharedFavorites(@PathVariable Long matchId) {
        return sharedFavoriteService.findAll(matchId, currentUserId());
    }

    @Operation(summary = "Добавить объявление в общее избранное")
    @PostMapping("/{matchId}/favorites")
    @ResponseStatus(HttpStatus.CREATED)
    public RoommateSharedFavoriteResponse addSharedFavorite(
        @PathVariable Long matchId, @Valid @RequestBody RoommateSharedFavoriteCreateDto request) {
        return sharedFavoriteService.add(matchId, currentUserId(), request);
    }

    @Operation(summary = "Изменить статус общего избранного")
    @PutMapping("/{matchId}/favorites/{listingId}")
    public RoommateSharedFavoriteResponse updateSharedFavorite(
        @PathVariable Long matchId, @PathVariable Long listingId, @Valid @RequestBody RoommateSharedFavoriteUpdateDto request) {
        return sharedFavoriteService.updateStatus(matchId, currentUserId(), listingId, request);
    }

    @Operation(summary = "Убрать объявление из общего избранного")
    @DeleteMapping("/{matchId}/favorites/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSharedFavorite(@PathVariable Long matchId, @PathVariable Long listingId) {
        sharedFavoriteService.remove(matchId, currentUserId(), listingId);
    }

    private Long currentUserId() {
        return userSessionService.getCurrentUser().getUserId();
    }
}
