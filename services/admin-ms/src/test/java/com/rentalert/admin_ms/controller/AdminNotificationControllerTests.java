package com.rentalert.admin_ms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.client.NotificationAdminClient;
import com.rentalert.admin_ms.client.UserBriefs;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.dto.notification.BroadcastRecipientStatus;
import com.rentalert.dto.notification.BroadcastRequest;
import com.rentalert.dto.notification.BroadcastResponse;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.service.UserSessionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class AdminNotificationControllerTests {

    private final NotificationAdminClient client = mock(NotificationAdminClient.class);
    private final UserBriefs userBriefs = mock(UserBriefs.class);
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private final AdminAuditPublisher auditPublisher = mock(AdminAuditPublisher.class);
    private final AdminNotificationController controller =
        new AdminNotificationController(client, userBriefs, userSessionService, auditPublisher);

    @Test
    void broadcastIsCreatedByCurrentAdminAndAudited() {
        BroadcastRequest request = BroadcastRequest.builder().message("Привет").build();
        BroadcastResponse accepted = BroadcastResponse.builder().broadcastId(10L).totalRecipients(3).build();
        when(userSessionService.getCurrentUser()).thenReturn(UserSession.builder().userId(99L).build());
        when(client.broadcast(request, 99L)).thenReturn(accepted);

        assertThat(controller.broadcast(request)).isSameAs(accepted);
        verify(auditPublisher).publish(AdminEvent.NOTIFICATION_SEND);
    }

    @Test
    void recipientsGetUsernamesAndPhotos() {
        BroadcastRecipientResponse recipient = new BroadcastRecipientResponse(1L, 7L, null, null, BroadcastRecipientStatus.SENT, null, null);
        when(client.findRecipients(10L, null, 0, 50, List.of())).thenReturn(new PageResponse<>(List.of(recipient), 0, 50, 1, 1));
        when(userBriefs.byId(List.of(7L))).thenReturn(Map.of(7L, new UserBriefResponse(7L, "ivan", "https://t.me/i/ivan.jpg")));

        PageResponse<BroadcastRecipientResponse> page = controller.findRecipients(10L, null, PageRequest.of(0, 50));

        assertThat(page.content()).singleElement().satisfies(r -> {
            assertThat(r.username()).isEqualTo("ivan");
            assertThat(r.photoUrl()).isEqualTo("https://t.me/i/ivan.jpg");
            assertThat(r.status()).isEqualTo(BroadcastRecipientStatus.SENT);
        });
        assertThat(page.totalElements()).isEqualTo(1);
    }
}
