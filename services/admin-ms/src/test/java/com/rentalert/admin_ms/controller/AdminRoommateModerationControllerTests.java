package com.rentalert.admin_ms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.exception.ProxiedHttpException;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.dto.roommate.RoommateModerationDecisionRequest;
import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import com.rentalert.internal.client.service.RoommateMsService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class AdminRoommateModerationControllerTests {

    private final RoommateMsService client = mock(RoommateMsService.class);
    private final AdminAuditPublisher auditPublisher = mock(AdminAuditPublisher.class);
    private final AdminRoommateModerationController controller = new AdminRoommateModerationController(client, auditPublisher);

    private final RoommateModerationTaskResponse task = new RoommateModerationTaskResponse(
        7L, ModerationSubjectType.PROFILE, 5L, "bio", "text", ModerationStatus.APPROVED, 1L, null, null);

    @Test
    void approvalIsForwardedAndAudited() {
        RoommateModerationDecisionRequest request = new RoommateModerationDecisionRequest("ok");
        when(client.approveModeration(7L, request)).thenReturn(task);

        assertThat(controller.approve(7L, request)).isSameAs(task);
        verify(auditPublisher).publish(AdminEvent.ROOMMATE_MODERATION_APPROVE, 7L);
    }

    @Test
    void rejectionWithoutBodySendsEmptyReason() {
        when(client.rejectModeration(7L, new RoommateModerationDecisionRequest(null))).thenReturn(task);

        assertThat(controller.reject(7L, null)).isSameAs(task);
        verify(auditPublisher).publish(AdminEvent.ROOMMATE_MODERATION_REJECT, 7L);
    }

    @Test
    void failedDecisionIsNotAudited() {
        when(client.approveModeration(any(), any())).thenThrow(new ProxiedHttpException("resolved", 409, "{}"));

        assertThatThrownBy(() -> controller.approve(7L, null)).isInstanceOf(ProxiedHttpException.class);
        verifyNoInteractions(auditPublisher);
    }

    @Test
    void queuePagingIsForwarded() {
        PageResponse<RoommateModerationTaskResponse> page = new PageResponse<>(List.of(task), 1, 20, 21, 2);
        PageRequest pageable = PageRequest.of(1, 20);
        when(client.findModerationQueue(pageable)).thenReturn(page);

        assertThat(controller.queue(pageable)).isSameAs(page);
    }
}
