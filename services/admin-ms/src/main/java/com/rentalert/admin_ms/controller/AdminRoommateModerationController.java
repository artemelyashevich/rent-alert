package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.roommate.RoommateModerationDecisionRequest;
import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import com.rentalert.internal.client.service.RoommateMsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin roommate moderation", description = "Модерация раздела «Соседи»")
@RestController
@RequestMapping(PublicUrls.ROOMMATE_MODERATION_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRoommateModerationController {

    private final RoommateMsService roommateMsService;
    private final AdminAuditPublisher auditPublisher;

    @Operation(summary = "Очередь ручной модерации")
    @GetMapping("/queue")
    public PageResponse<RoommateModerationTaskResponse> queue(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return roommateMsService.findModerationQueue(pageable);
    }

    @Operation(
        summary = "Одобрить задачу",
        description = "Для текстовых полей — публикует текст. Для жалоб — подтверждает нарушение и блокирует участие."
    )
    @PostMapping("/{taskId}/approve")
    public RoommateModerationTaskResponse approve(@PathVariable Long taskId,
        @RequestBody(required = false) RoommateModerationDecisionRequest request) {
        RoommateModerationTaskResponse task = roommateMsService.approveModeration(taskId, orEmpty(request));
        auditPublisher.publish(AdminEvent.ROOMMATE_MODERATION_APPROVE, taskId);
        return task;
    }

    @Operation(
        summary = "Отклонить задачу",
        description = "Для текстовых полей — прежний одобренный текст остаётся видимым. Для жалоб — снимает ограничения."
    )
    @PostMapping("/{taskId}/reject")
    public RoommateModerationTaskResponse reject(@PathVariable Long taskId,
        @RequestBody(required = false) RoommateModerationDecisionRequest request) {
        RoommateModerationTaskResponse task = roommateMsService.rejectModeration(taskId, orEmpty(request));
        auditPublisher.publish(AdminEvent.ROOMMATE_MODERATION_REJECT, taskId);
        return task;
    }

    private static RoommateModerationDecisionRequest orEmpty(RoommateModerationDecisionRequest request) {
        return request == null ? new RoommateModerationDecisionRequest(null) : request;
    }
}
