package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.client.NotificationAdminClient;
import com.rentalert.admin_ms.client.UserBriefs;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.dto.notification.BroadcastRequest;
import com.rentalert.dto.notification.BroadcastResponse;
import com.rentalert.dto.notification.BroadcastSummaryResponse;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.redis.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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

@Tag(name = "Admin notifications", description = "Рассылки администратора в Telegram")
@RestController
@RequestMapping(PublicUrls.NOTIFICATION_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationAdminClient notificationAdminClient;
    private final UserBriefs userBriefs;
    private final UserSessionService userSessionService;
    private final AdminAuditPublisher auditPublisher;

    private static BroadcastSummaryResponse withAuthor(BroadcastSummaryResponse summary, Map<Long, UserBriefResponse> briefs) {
        UserBriefResponse author = briefs.get(summary.createdByUserId());
        if (author == null) {
            return summary;
        }
        return new BroadcastSummaryResponse(summary.id(), summary.message(), summary.createdByUserId(), author.username(),
            summary.totalRecipients(), summary.sent(), summary.failed(), summary.skipped(), summary.pinned(), summary.createdAt());
    }

    private static BroadcastRecipientResponse withUser(BroadcastRecipientResponse recipient, Map<Long, UserBriefResponse> briefs) {
        UserBriefResponse user = briefs.get(recipient.userId());
        if (user == null) {
            return recipient;
        }
        return new BroadcastRecipientResponse(recipient.id(), recipient.userId(), user.username(), user.photoUrl(),
            recipient.status(), recipient.errorMessage(), recipient.createdAt());
    }

    @Operation(summary = "Запустить рассылку: всем пользователям, кроме администраторов, или выбранным userIds. "
        + "Отправка идёт в фоне, прогресс — в GET /broadcasts/{id}")
    @PostMapping("/broadcast")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BroadcastResponse broadcast(@Valid @RequestBody BroadcastRequest request) {
        BroadcastResponse response = notificationAdminClient.broadcast(request, userSessionService.getCurrentUser().getUserId());
        auditPublisher.publish(AdminEvent.NOTIFICATION_SEND);
        return response;
    }

    @Operation(summary = "Список рассылок")
    @GetMapping("/broadcasts")
    public PageResponse<BroadcastSummaryResponse> findAll(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<BroadcastSummaryResponse> page = notificationAdminClient.findBroadcasts(
            pageable.getPageNumber(), pageable.getPageSize(), PageUtil.toSortParams(pageable));
        Map<Long, UserBriefResponse> briefs = userBriefs.byId(page.content().stream().map(BroadcastSummaryResponse::createdByUserId).toList());
        List<BroadcastSummaryResponse> content = page.content().stream().map(summary -> withAuthor(summary, briefs)).toList();
        return new PageResponse<>(content, page.number(), page.size(), page.totalElements(), page.totalPages());
    }

    @Operation(summary = "Рассылка и её счётчики")
    @GetMapping("/broadcasts/{broadcastId}")
    public BroadcastSummaryResponse findById(@PathVariable Long broadcastId) {
        BroadcastSummaryResponse summary = notificationAdminClient.findBroadcast(broadcastId);
        return withAuthor(summary, userBriefs.byId(Collections.singletonList(summary.createdByUserId())));
    }

    @Operation(summary = "Получатели рассылки; search — id пользователя")
    @GetMapping("/broadcasts/{broadcastId}/recipients")
    public PageResponse<BroadcastRecipientResponse> findRecipients(
        @PathVariable Long broadcastId,
        @RequestParam(required = false) Long search,
        @ParameterObject @PageableDefault(size = 50) Pageable pageable
    ) {
        PageResponse<BroadcastRecipientResponse> page = notificationAdminClient.findRecipients(broadcastId, search,
            pageable.getPageNumber(), pageable.getPageSize(), PageUtil.toSortParams(pageable));
        Map<Long, UserBriefResponse> briefs = userBriefs.byId(page.content().stream().map(BroadcastRecipientResponse::userId).toList());
        List<BroadcastRecipientResponse> content = page.content().stream().map(recipient -> withUser(recipient, briefs)).toList();
        return new PageResponse<>(content, page.number(), page.size(), page.totalElements(), page.totalPages());
    }

    @Operation(summary = "Удалить рассылку вместе со списком получателей")
    @DeleteMapping("/broadcasts/{broadcastId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long broadcastId) {
        notificationAdminClient.deleteBroadcast(broadcastId);
        auditPublisher.publish(AdminEvent.NOTIFICATION_DELETE, broadcastId);
    }
}
