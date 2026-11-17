package com.rentalert.roommate_ms.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.notification.NotificationDelivery;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class RoommateNotificationPublisherTests {

    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final RoommateNotificationPublisher publisher = createPublisher();

    private RoommateNotificationPublisher createPublisher() {
        RoommateNotificationPublisher created = new RoommateNotificationPublisher(identityMsService);
        ReflectionTestUtils.setField(created, "outboxPublisher", outboxPublisher);
        return created;
    }

    @Test
    void requestCarriesReceiverAndQuietHoursEnd() {
        NotificationRequest.Receiver receiver = new NotificationRequest.Receiver("tg-enc", null);
        Instant resumeAt = Instant.parse("2026-10-02T05:00:00Z");
        when(identityMsService.getDelivery(5L)).thenReturn(new NotificationDelivery(receiver, resumeAt));

        publisher.publish(5L, NotificationType.ROOMMATE_PROFILE_APPROVED, "ok", "roommate-moderation:7");

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.NOTIFICATION_REQUESTS), eq("5"), captor.capture());
        assertThat(captor.getValue())
            .satisfies(request -> {
                assertThat(request.producer()).isEqualTo("roommate-ms");
                assertThat(request.userId()).isEqualTo(5L);
                assertThat(request.type()).isEqualTo(NotificationType.ROOMMATE_PROFILE_APPROVED);
                assertThat(request.templateParams()).isEqualTo(Map.of("message", "ok"));
                assertThat(request.deduplicationKey()).isEqualTo("roommate-moderation:7");
                assertThat(request.receiver()).isEqualTo(receiver);
                assertThat(request.sendAt()).isEqualTo(resumeAt);
            });
    }

    @Test
    void notBeforeDefersBeyondQuietHoursEnd() {
        NotificationRequest.Receiver receiver = new NotificationRequest.Receiver("tg-enc", null);
        Instant quietEnd = Instant.parse("2026-10-02T05:00:00Z");
        Instant reminderAt = Instant.parse("2026-10-03T12:00:00Z");
        when(identityMsService.getDelivery(5L)).thenReturn(new NotificationDelivery(receiver, quietEnd));

        publisher.publish(5L, NotificationType.ROOMMATE_VIEWING_REMINDER, Map.of("message", "soon"), "reminder:1", reminderAt);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.notificationRequests(NotificationType.ROOMMATE_VIEWING_REMINDER)),
            eq("5"), captor.capture());
        assertThat(captor.getValue().sendAt()).isEqualTo(reminderAt);
    }

    @Test
    void templateParamsAreSentAsGiven() {
        NotificationRequest.Receiver receiver = new NotificationRequest.Receiver("tg-enc", null);
        when(identityMsService.getDelivery(5L)).thenReturn(new NotificationDelivery(receiver, null));

        publisher.publish(5L, NotificationType.ROOMMATE_MATCHED, Map.of("message", "hi", "matchId", "100"), "key", null);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.notificationRequests(NotificationType.ROOMMATE_MATCHED)),
            eq("5"), captor.capture());
        assertThat(captor.getValue().templateParams()).isEqualTo(Map.of("message", "hi", "matchId", "100"));
        assertThat(captor.getValue().sendAt()).isNull();
    }

    @Test
    void failedDeliveryLookupSkipsNotification() {
        when(identityMsService.getDelivery(5L)).thenThrow(new UpstreamHttpException("unavailable", 503, null));

        publisher.publish(5L, NotificationType.ROOMMATE_PROFILE_REJECTED, "no", "roommate-moderation:7");

        verifyNoInteractions(outboxPublisher);
    }
}
