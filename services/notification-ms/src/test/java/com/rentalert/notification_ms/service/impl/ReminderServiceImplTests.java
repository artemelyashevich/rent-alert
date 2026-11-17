package com.rentalert.notification_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.dto.listing.FavoriteViewingDto;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.ReminderIntervalType;
import com.rentalert.dto.roommate.RoommateViewingDto;
import com.rentalert.dto.roommate.RoommateViewingStatus;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.internal.client.service.RoommateMsService;
import com.rentalert.notification_ms.domain.NotificationTemplate;
import com.rentalert.notification_ms.domain.Reminder;
import com.rentalert.notification_ms.service.NotificationService;
import com.rentalert.notification_ms.service.ReminderPersistenceService;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ReminderServiceImplTests {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final ReminderPersistenceService persistenceService = mock(ReminderPersistenceService.class);
    private final ListingMsService listingMsService = mock(ListingMsService.class);
    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final CryptoService cryptoService = mock(CryptoService.class);
    private final RoommateMsService roommateMsService = mock(RoommateMsService.class);
    private final ReminderServiceImpl service = new ReminderServiceImpl(
        notificationService, persistenceService, listingMsService, identityMsService, cryptoService, roommateMsService);

    private final Reminder reminder = Reminder.builder()
        .userId(42L)
        .listingId(15L)
        .intervalType(ReminderIntervalType.BEFORE_1_HOUR)
        .notificationTemplate(NotificationTemplate.builder().engine(NotificationEngine.TELEGRAM_BOT).build())
        .build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "botZone", ZoneId.of("Europe/Minsk"));
        when(persistenceService.findBatchToRemind(any(Integer.class))).thenReturn(List.of(reminder));
    }

    @Test
    void sendsToDecryptedTelegramIdWithListingCard() {
        ListingDto listing = ListingDto.builder().title("2-комн.").street("Немига 5").sourceUrl("https://kufar.by/1").build();
        when(listingMsService.getFavoriteViewing(42L, 15L)).thenReturn(Optional.of(
            new FavoriteViewingDto(42L, 15L, Instant.parse("2026-10-05T12:00:00Z"), listing)));
        when(identityMsService.getReceiver(42L)).thenReturn(new NotificationRequest.Receiver("tg-enc", null));
        when(cryptoService.decrypt("tg-enc")).thenReturn("123456");

        service.send();

        ArgumentCaptor<NotificationDto> sent = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationService).sendNotification(sent.capture());
        assertThat(sent.getValue().getReceiver()).isEqualTo("123456");
        assertThat(sent.getValue().getEngine()).isEqualTo(NotificationEngine.TELEGRAM_BOT);
        assertThat(sent.getValue().getTemplateParams().get("message"))
            .contains("5 октября, 15:00", "напоминание за час", "2-комн. // Немига 5", "https://kufar.by/1");
        verify(persistenceService).markAsSent(reminder);
    }

    @Test
    void removedFavoriteFailsReminder() {
        when(listingMsService.getFavoriteViewing(42L, 15L)).thenReturn(Optional.empty());

        service.send();

        verify(notificationService, never()).sendNotification(any());
        verify(persistenceService).markAsFailed(any(), anyString());
    }

    @Test
    void missingTelegramIdFailsReminder() {
        when(listingMsService.getFavoriteViewing(42L, 15L)).thenReturn(Optional.of(
            new FavoriteViewingDto(42L, 15L, Instant.parse("2026-10-05T12:00:00Z"), null)));
        when(identityMsService.getReceiver(42L)).thenReturn(new NotificationRequest.Receiver(null, null));

        service.send();

        verify(notificationService, never()).sendNotification(any());
        verify(persistenceService).markAsFailed(any(), anyString());
    }

    @Test
    void sendsRoommateViewingReminderWithSnapshotCard() {
        Reminder roommateReminder = roommateReminder();
        when(persistenceService.findBatchToRemind(any(Integer.class))).thenReturn(List.of(roommateReminder));
        when(roommateMsService.getViewing(5L)).thenReturn(Optional.of(scheduledViewing()));
        when(identityMsService.getReceiver(42L)).thenReturn(new NotificationRequest.Receiver("tg-enc", null));
        when(cryptoService.decrypt("tg-enc")).thenReturn("123456");

        service.send();

        ArgumentCaptor<NotificationDto> sent = ArgumentCaptor.forClass(NotificationDto.class);
        verify(notificationService).sendNotification(sent.capture());
        assertThat(sent.getValue().getReceiver()).isEqualTo("123456");
        assertThat(sent.getValue().getTemplateParams().get("message"))
            .contains("5 октября, 15:00", "напоминание за час", "2-комн. // Немига 5");
        verify(listingMsService, never()).getFavoriteViewing(any(), any());
        verify(persistenceService).markAsSent(roommateReminder);
    }

    @Test
    void removedRoommateViewingFailsReminder() {
        when(persistenceService.findBatchToRemind(any(Integer.class))).thenReturn(List.of(roommateReminder()));
        when(roommateMsService.getViewing(5L)).thenReturn(Optional.empty());

        service.send();

        verify(notificationService, never()).sendNotification(any());
        verify(persistenceService).markAsFailed(any(), anyString());
    }

    @Test
    void cancelledRoommateViewingFailsReminder() {
        when(persistenceService.findBatchToRemind(any(Integer.class))).thenReturn(List.of(roommateReminder()));
        when(roommateMsService.getViewing(5L)).thenReturn(Optional.of(new RoommateViewingDto(5L, 10L, 42L, 77L,
            Instant.parse("2026-10-05T12:00:00Z"), RoommateViewingStatus.CANCELLED, 15L, "Немига 5", "2-комн.", "Немига 5")));

        service.send();

        verify(notificationService, never()).sendNotification(any());
        verify(persistenceService).markAsFailed(any(), anyString());
    }

    private static Reminder roommateReminder() {
        return Reminder.builder()
            .userId(42L)
            .roommateViewingId(5L)
            .intervalType(ReminderIntervalType.BEFORE_1_HOUR)
            .notificationTemplate(NotificationTemplate.builder().engine(NotificationEngine.TELEGRAM_BOT).build())
            .build();
    }

    private static RoommateViewingDto scheduledViewing() {
        return new RoommateViewingDto(5L, 10L, 42L, 77L, Instant.parse("2026-10-05T12:00:00Z"),
            RoommateViewingStatus.SCHEDULED, 15L, "Немига 5", "2-комн.", "Немига 5");
    }
}
