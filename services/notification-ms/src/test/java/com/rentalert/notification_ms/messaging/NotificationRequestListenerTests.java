package com.rentalert.notification_ms.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.notification_ms.domain.Notification;
import com.rentalert.notification_ms.service.NotificationService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationRequestListenerTests {

    private static final Duration GRACE = Duration.ofSeconds(30);

    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationRequestListener listener = new NotificationRequestListener(notificationService, GRACE);

    private static NotificationRequest request(NotificationType type, Instant expiresAt) {
        return NotificationRequest.builder().userId(42L).type(type).expiresAt(expiresAt).build();
    }

    @Test
    void queuesRequestForImmediateDispatcherPickup() {
        NotificationRequest request = request(NotificationType.NEW_LISTING_MATCHED, null);

        listener.onRequest(request);

        assertThat(capturedSendAt(request)).isCloseTo(Instant.now(), within(Duration.ofSeconds(5)));
        verify(notificationService, never()).dispatch(any());
    }

    @Test
    void priorityRequestIsSentRightAwayAndHiddenFromDispatcherForGracePeriod() {
        NotificationRequest request = request(NotificationType.EMAIL_ACTIVATION, null);
        List<Notification> queued = List.of(new Notification());
        when(notificationService.enqueue(eq(request), any())).thenReturn(queued);

        listener.onPriorityRequest(request);

        assertThat(capturedSendAt(request)).isCloseTo(Instant.now().plus(GRACE), within(Duration.ofSeconds(5)));
        verify(notificationService).dispatch(queued);
    }

    @Test
    void requestForQuietHoursIsQueuedUntilItsSendTime() {
        Instant sendAt = Instant.now().plus(Duration.ofHours(8));
        NotificationRequest request = NotificationRequest.builder()
            .userId(42L).type(NotificationType.NEW_LISTING_MATCHED).sendAt(sendAt).build();

        listener.onRequest(request);

        assertThat(capturedSendAt(request)).isEqualTo(sendAt);
        verify(notificationService, never()).dispatch(any());
    }

    @Test
    void deferredPriorityRequestIsNotSentRightAway() {
        Instant sendAt = Instant.now().plus(Duration.ofHours(8));
        NotificationRequest request = NotificationRequest.builder()
            .userId(42L).type(NotificationType.EMAIL_ACTIVATION).sendAt(sendAt).build();

        listener.onPriorityRequest(request);

        assertThat(capturedSendAt(request)).isEqualTo(sendAt);
        verify(notificationService, never()).dispatch(any());
    }

    @Test
    void sendTimeInThePastIsSentRightAway() {
        NotificationRequest request = NotificationRequest.builder()
            .userId(42L).type(NotificationType.NEW_LISTING_MATCHED).sendAt(Instant.now().minusSeconds(60)).build();

        listener.onRequest(request);

        assertThat(capturedSendAt(request)).isCloseTo(Instant.now(), within(Duration.ofSeconds(5)));
    }

    @Test
    void expiredRequestIsDropped() {
        listener.onPriorityRequest(request(NotificationType.EMAIL_ACTIVATION, Instant.now().minusSeconds(1)));

        verify(notificationService, never()).enqueue(any(), any());
    }

    @Test
    void requestWithoutTypeIsNotRetried() {
        assertThatThrownBy(() -> listener.onRequest(request(null, null)))
            .isInstanceOf(NonRetryableEventException.class);
        verify(notificationService, never()).enqueue(any(), any());
    }

    @Test
    void requestWithoutReceiverAddressIsNotRetried() {
        NotificationRequest request = request(NotificationType.NEW_LISTING_MATCHED, null);
        when(notificationService.enqueue(eq(request), any())).thenThrow(new IllegalArgumentException("no address"));

        assertThatThrownBy(() -> listener.onRequest(request)).isInstanceOf(NonRetryableEventException.class);
    }

    private Instant capturedSendAt(NotificationRequest request) {
        ArgumentCaptor<Instant> sendAt = ArgumentCaptor.forClass(Instant.class);
        verify(notificationService).enqueue(eq(request), sendAt.capture());
        return sendAt.getValue();
    }
}
