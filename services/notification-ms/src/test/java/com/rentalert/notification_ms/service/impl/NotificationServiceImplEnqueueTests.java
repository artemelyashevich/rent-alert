package com.rentalert.notification_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.util.auth.CryptoService;
import com.rentalert.common.util.auth.HmacBlindIndexService;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.domain.Notification;
import com.rentalert.notification_ms.domain.NotificationStatus;
import com.rentalert.notification_ms.domain.UserNotificationSetting;
import com.rentalert.notification_ms.mapper.NotificationMapper;
import com.rentalert.notification_ms.repository.NotificationRepository;
import com.rentalert.notification_ms.repository.NotificationTemplateRepository;
import com.rentalert.notification_ms.repository.UserNotificationSettingRepository;
import com.rentalert.notification_ms.service.NotificationTypeSettings;
import com.rentalert.notification_ms.service.NotificationTypeSettingsService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceImplEnqueueTests {

    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final UserNotificationSettingRepository settings = mock(UserNotificationSettingRepository.class);
    private final CryptoService crypto = mock(CryptoService.class);
    private final HmacBlindIndexService blindIndex = mock(HmacBlindIndexService.class);
    private final NotificationTypeSettingsService typeSettings = mock(NotificationTypeSettingsService.class);

    private final NotificationServiceImpl service = new NotificationServiceImpl(List.of(), notifications,
        mock(NotificationTemplateRepository.class), settings, mock(NotificationMapper.class), crypto, blindIndex,
        typeSettings);

    private final Instant sendAt = Instant.parse("2026-09-29T10:00:00Z");

    private static NotificationRequest listingMatched(NotificationRequest.Receiver receiver) {
        return NotificationRequest.builder()
            .userId(42L)
            .type(NotificationType.NEW_LISTING_MATCHED)
            .templateParams(Map.of("city", "Минск"))
            .deduplicationKey("listing:1:user:42")
            .listingId(1L)
            .receiver(receiver)
            .build();
    }

    @BeforeEach
    void setUp() {
        when(notifications.save(any())).thenAnswer(returnsFirstArg());
        when(settings.findByUserIdAndEngine(any(), any())).thenReturn(Optional.empty());
        when(crypto.decrypt("enc-chat")).thenReturn("123456");
        when(blindIndex.hash(anyString())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        typeSettings(NotificationType.NEW_LISTING_MATCHED, NotificationEngine.TELEGRAM_BOT, false, true);
        typeSettings(NotificationType.USER_CREATED, NotificationEngine.TELEGRAM_ADMIN, false, true);
    }

    private void typeSettings(NotificationType type, NotificationEngine engine, boolean mandatory, boolean enabled) {
        when(typeSettings.get(type))
            .thenReturn(new NotificationTypeSettings(type, "CATEGORY", List.of(engine), mandatory, enabled));
    }

    @Test
    void queuesOneRowPerChannelWithDecryptedAddress() {
        NotificationRequest request = listingMatched(new NotificationRequest.Receiver("enc-chat", null));

        List<Notification> queued = service.enqueue(request, sendAt);

        assertThat(queued).singleElement().satisfies(n -> {
            assertThat(n.getRequestId()).isEqualTo(request.requestId());
            assertThat(n.getUserId()).isEqualTo(42L);
            assertThat(n.getEngine()).isEqualTo(NotificationEngine.TELEGRAM_BOT);
            assertThat(n.getReceiver()).isEqualTo("123456");
            assertThat(n.getStatus()).isEqualTo(NotificationStatus.QUEUED);
            assertThat(n.getAttempt()).isZero();
            assertThat(n.getNextRetryAt()).isEqualTo(sendAt);
            assertThat(n.getDeduplicationKey()).isEqualTo("hash:listing:1:user:42");
            assertThat(n.getListingId()).isEqualTo(1L);
            assertThat(n.getTemplateParamsJson()).contains("\"city\"");
        });
    }

    @Test
    void redeliveredRequestIsNotQueuedAgain() {
        NotificationRequest request = listingMatched(new NotificationRequest.Receiver("enc-chat", null));
        when(notifications.existsByRequestId(request.requestId())).thenReturn(true);

        assertThat(service.enqueue(request, sendAt)).isEmpty();
        verify(notifications, never()).save(any());
    }

    @Test
    void alreadyDeliveredChannelIsSkipped() {
        when(notifications.existsByDeduplicationKeyAndEngineAndStatusIn(eq("hash:listing:1:user:42"),
            eq(NotificationEngine.TELEGRAM_BOT), anyList())).thenReturn(true);

        assertThat(service.enqueue(listingMatched(new NotificationRequest.Receiver("enc-chat", null)), sendAt)).isEmpty();
    }

    @Test
    void channelDisabledByUserIsSkipped() {
        UserNotificationSetting disabled = mock(UserNotificationSetting.class);
        when(settings.findByUserIdAndEngine(42L, NotificationEngine.TELEGRAM_BOT)).thenReturn(Optional.of(disabled));

        assertThat(service.enqueue(listingMatched(new NotificationRequest.Receiver("enc-chat", null)), sendAt)).isEmpty();
    }

    @Test
    void disabledTypeIsNotQueued() {
        typeSettings(NotificationType.NEW_LISTING_MATCHED, NotificationEngine.TELEGRAM_BOT, false, false);

        assertThat(service.enqueue(listingMatched(new NotificationRequest.Receiver("enc-chat", null)), sendAt)).isEmpty();
    }

    @Test
    void mandatoryTypeIgnoresChannelDisabledByUser() {
        typeSettings(NotificationType.NEW_LISTING_MATCHED, NotificationEngine.TELEGRAM_BOT, true, true);
        UserNotificationSetting disabled = mock(UserNotificationSetting.class);
        when(settings.findByUserIdAndEngine(42L, NotificationEngine.TELEGRAM_BOT)).thenReturn(Optional.of(disabled));

        assertThat(service.enqueue(listingMatched(new NotificationRequest.Receiver("enc-chat", null)), sendAt)).hasSize(1);
    }

    @Test
    void missingAddressIsRejected() {
        assertThatThrownBy(() -> service.enqueue(listingMatched(null), sendAt))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adminNotificationNeedsNoAddress() {
        NotificationRequest request = NotificationRequest.builder().type(NotificationType.USER_CREATED).build();

        assertThat(service.enqueue(request, sendAt)).singleElement()
            .satisfies(n -> assertThat(n.getReceiver()).isEqualTo("[internal]"));
    }
}
