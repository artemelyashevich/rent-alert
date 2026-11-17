package com.rentalert.notification_ms.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.RealtimePushEvent;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.kafka.outbox.OutboxPublisher;
import com.rentalert.notification_ms.messaging.RealtimePushPublisher;
import com.rentalert.notification_ms.service.NotificationTypeSettings;
import com.rentalert.notification_ms.service.NotificationTypeSettingsService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class WebSocketNotificationStrategyTests {

    private final NotificationTypeSettingsService typeSettingsService = mock(NotificationTypeSettingsService.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private WebSocketNotificationStrategy strategy;

    private static NotificationDto dto(Long userId) {
        return NotificationDto.builder()
            .type(NotificationType.ROOMMATE_NEW_INTEREST)
            .userId(userId)
            .receiver("[internal]")
            .content("New interest")
            .listingId(7L)
            .templateParams(Map.of("name", "Anna"))
            .build();
    }

    @BeforeEach
    void setUp() {
        RealtimePushPublisher realtimePushPublisher = new RealtimePushPublisher();
        ReflectionTestUtils.setField(realtimePushPublisher, "outboxPublisher", outboxPublisher);
        strategy = new WebSocketNotificationStrategy(typeSettingsService, realtimePushPublisher, new TransactionTemplate(transactionManager));
        ReflectionTestUtils.setField(strategy, "enabled", true);
        when(typeSettingsService.get(NotificationType.ROOMMATE_NEW_INTEREST)).thenReturn(
            new NotificationTypeSettings(NotificationType.ROOMMATE_NEW_INTEREST, "ROOMMATE_ALERTS",
                List.of(NotificationEngine.WEBSOCKET), false, true));
    }

    @Test
    void publishesRealtimePushEventKeyedByUserIdInTransaction() {
        NotificationDto dto = dto(42L);

        strategy.sendNotification(dto);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.REALTIME_PUSH), eq("42"), captor.capture());
        RealtimePushEvent event = (RealtimePushEvent) captor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.userId()).isEqualTo("42");
        assertThat(event.payload().id()).isEqualTo(dto.getId());
        assertThat(event.payload().type()).isEqualTo(NotificationType.ROOMMATE_NEW_INTEREST);
        assertThat(event.payload().category()).isEqualTo("ROOMMATE_ALERTS");
        assertThat(event.payload().listingId()).isEqualTo(7L);
        assertThat(event.payload().body()).isEqualTo("New interest");
        assertThat(event.payload().params()).containsEntry("name", "Anna");
        assertThat(event.payload().createdAt()).isNotNull();
        verify(transactionManager).getTransaction(any());
        verify(transactionManager).commit(any());
    }

    @Test
    void publishesNothingWhenWsDisabled() {
        ReflectionTestUtils.setField(strategy, "enabled", false);

        strategy.sendNotification(dto(42L));

        verifyNoInteractions(outboxPublisher);
        verifyNoInteractions(transactionManager);
    }

    @Test
    void publishesNothingWithoutUserId() {
        strategy.sendNotification(dto(null));

        verifyNoInteractions(outboxPublisher);
    }
}
