package com.rentalert.realtime_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.RealtimePushEvent;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.notification.RealtimeNotificationPayload;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.realtime_ms.service.RecentEventCache;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RealtimePushListenerTests {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final RecentEventCache recentEventCache = new RecentEventCache(100, Duration.ofMinutes(10), Clock.systemUTC());
    private final RealtimePushListener listener = new RealtimePushListener(recentEventCache, messagingTemplate);

    private static RealtimeNotificationPayload payload() {
        return new RealtimeNotificationPayload("n-1", NotificationType.NEW_LISTING_MATCHED, "LISTINGS", 7L, "body",
            Map.of("city", "Minsk"), Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void deliversToUserDestination() {
        RealtimePushEvent event = new RealtimePushEvent(UUID.randomUUID(), "5", payload());

        listener.onRealtimePush(event);

        verify(messagingTemplate).convertAndSendToUser("5", RealtimePushListener.USER_DESTINATION, event.payload());
    }

    @Test
    void redeliveredEventIsNotSentAgain() {
        RealtimePushEvent event = new RealtimePushEvent(UUID.randomUUID(), "5", payload());

        listener.onRealtimePush(event);
        listener.onRealtimePush(event);

        verify(messagingTemplate, times(1)).convertAndSendToUser(eq("5"), any(), any());
    }

    @Test
    void eventWithoutEventIdIsNotRetried() {
        RealtimePushEvent event = new RealtimePushEvent(null, "5", payload());

        assertThatThrownBy(() -> listener.onRealtimePush(event))
            .isInstanceOf(NonRetryableEventException.class);
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void eventWithoutUserIdIsNotRetried() {
        RealtimePushEvent event = new RealtimePushEvent(UUID.randomUUID(), " ", payload());

        assertThatThrownBy(() -> listener.onRealtimePush(event))
            .isInstanceOf(NonRetryableEventException.class);
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void eventWithoutPayloadIsNotRetried() {
        RealtimePushEvent event = new RealtimePushEvent(UUID.randomUUID(), "5", null);

        assertThatThrownBy(() -> listener.onRealtimePush(event))
            .isInstanceOf(NonRetryableEventException.class);
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void nullEventIsNotRetried() {
        assertThatThrownBy(() -> listener.onRealtimePush(null))
            .isInstanceOf(NonRetryableEventException.class);
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }
}
