package com.rentalert.notification_ms.controller.internal;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.dto.notification.BroadcastRequest;
import com.rentalert.dto.notification.BroadcastResponse;
import com.rentalert.dto.notification.BroadcastSummaryResponse;
import com.rentalert.notification_ms.config.PublicUrls;
import com.rentalert.notification_ms.service.AdminBroadcastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG + "/broadcasts")
@RequiredArgsConstructor
public class AdminBroadcastInternalController {

    private final AdminBroadcastService adminBroadcastService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BroadcastResponse broadcast(@Valid @RequestBody BroadcastRequest request, @RequestParam Long createdBy) {
        return adminBroadcastService.broadcast(request.getMessage(), request.getUserIds(), request.isPin(), createdBy);
    }

    @GetMapping
    public PageResponse<BroadcastSummaryResponse> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return PageUtil.toResponse(adminBroadcastService.findAll(pageable));
    }

    @GetMapping("/{broadcastId}")
    public BroadcastSummaryResponse findById(@PathVariable Long broadcastId) {
        return adminBroadcastService.findById(broadcastId);
    }

    @GetMapping("/{broadcastId}/recipients")
    public PageResponse<BroadcastRecipientResponse> findRecipients(
        @PathVariable Long broadcastId,
        @RequestParam(required = false) Long userId,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        return PageUtil.toResponse(adminBroadcastService.findRecipients(broadcastId, userId, pageable));
    }

    @DeleteMapping("/{broadcastId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long broadcastId) {
        adminBroadcastService.delete(broadcastId);
    }
}
