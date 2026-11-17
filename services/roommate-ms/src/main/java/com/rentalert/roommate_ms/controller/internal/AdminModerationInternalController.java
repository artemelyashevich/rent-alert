package com.rentalert.roommate_ms.controller.internal;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.roommate.RoommateModerationDecisionRequest;
import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import com.rentalert.redis.service.UserSessionService;
import com.rentalert.roommate_ms.config.PublicUrls;
import com.rentalert.roommate_ms.service.RoommateModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(PublicUrls.INTERNAL_ADMIN_MODERATION_TAG)
@RequiredArgsConstructor
public class AdminModerationInternalController {

    private final RoommateModerationService moderationService;
    private final UserSessionService userSessionService;

    @GetMapping("/queue")
    public PageResponse<RoommateModerationTaskResponse> queue(Pageable pageable) {
        return PageUtil.toResponse(moderationService.queue(pageable));
    }

    @PostMapping("/{taskId}/approve")
    public RoommateModerationTaskResponse approve(@PathVariable Long taskId,
        @RequestBody(required = false) RoommateModerationDecisionRequest request) {
        return moderationService.approve(taskId, currentAdminId(), reasonOf(request));
    }

    @PostMapping("/{taskId}/reject")
    public RoommateModerationTaskResponse reject(@PathVariable Long taskId,
        @RequestBody(required = false) RoommateModerationDecisionRequest request) {
        return moderationService.reject(taskId, currentAdminId(), reasonOf(request));
    }

    private static String reasonOf(RoommateModerationDecisionRequest request) {
        return request == null ? null : request.reason();
    }

    private Long currentAdminId() {
        return userSessionService.getCurrentUser().getUserId();
    }
}
