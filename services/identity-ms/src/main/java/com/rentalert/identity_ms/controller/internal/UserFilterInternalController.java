package com.rentalert.identity_ms.controller.internal;

import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.identity_ms.config.PublicUrls;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.identity_ms.mapper.UserFilterMapper;
import com.rentalert.identity_ms.service.UserFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_FILTER_TAG)
@RequiredArgsConstructor
public class UserFilterInternalController {

    private final UserFilterService userFilterService;
    private final UserFilterMapper userFilterMapper;

    @GetMapping("/active/{userId}")
    public ResponseEntity<UserFilterResponse> findActiveFilter(@PathVariable Long userId) {
        UserFilter filter = userFilterService.findActiveFilterByUserId(userId);
        return filter == null
            ? ResponseEntity.noContent().build()
            : ResponseEntity.ok(userFilterMapper.toResponse(filter));
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<UserFilterResponse> findMatchFilter(@PathVariable Long matchId) {
        return userFilterService.findByOwnerMatchId(matchId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/match/{matchId}")
    public ResponseEntity<UserFilterResponse> upsertMatchFilter(
        @PathVariable Long matchId,
        @Valid @RequestBody UserFilterRequest request
    ) {
        return ResponseEntity.ok(userFilterService.upsertForMatch(matchId, request));
    }

    @PutMapping("/match/{matchId}/activation/{active}")
    public ResponseEntity<UserFilterResponse> setMatchFilterActivation(
        @PathVariable Long matchId,
        @PathVariable boolean active
    ) {
        return ResponseEntity.ok(userFilterService.setActivationForMatch(matchId, active));
    }
}
