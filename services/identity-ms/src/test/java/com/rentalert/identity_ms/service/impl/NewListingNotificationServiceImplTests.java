package com.rentalert.identity_ms.service.impl;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.dto.profile.UserRole;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.identity_ms.repository.UserFilterRepository;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewListingNotificationServiceImplTests {

    @Mock
    private UserFilterRepository userFilterRepository;

    @Mock
    private OutboxPublisher outboxPublisher;

    @Mock
    private QuietHoursPolicy quietHoursPolicy;

    @InjectMocks
    private NewListingNotificationServiceImpl service;

    @Test
    void publishesMatchedNotificationForEligibleUser() {
        UserProfile user = user(10L);
        when(userFilterRepository.findActiveFiltersByRoomsAndArea(2, 50f, "Минск", ListingCategory.FLAT))
            .thenReturn(List.of(filter(user, null, null, null)));

        service.notifyMatchingUsers(listing(ListingCategory.FLAT));

        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.NOTIFICATION_REQUESTS), eq("10"), request.capture());
        assertThat(request.getValue().type()).isEqualTo(NotificationType.NEW_LISTING_MATCHED);
        assertThat(request.getValue().userId()).isEqualTo(10L);
        assertThat(request.getValue().listingId()).isEqualTo(7L);
        assertThat(request.getValue().templateParams()).containsEntry("message", "new flat");
        assertThat(request.getValue().receiver().telegramIdEnc()).isEqualTo("enc-10");
        assertThat(request.getValue().deduplicationKey()).isEqualTo("listing:7:user:10");
        assertThat(request.getValue().sendAt()).isNull();
    }

    @Test
    void delaysNotificationUntilQuietHoursEnd() {
        UserProfile user = user(10L);
        Instant resumeAt = Instant.parse("2026-10-01T05:00:00Z");
        when(userFilterRepository.findActiveFiltersByRoomsAndArea(any(), any(), any(), any()))
            .thenReturn(List.of(filter(user, null, null, null)));
        when(quietHoursPolicy.resumeAt(eq(user.getSettings()), any())).thenReturn(Optional.of(resumeAt));

        service.notifyMatchingUsers(listing(ListingCategory.FLAT));

        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(outboxPublisher).publish(anyString(), eq("10"), request.capture());
        assertThat(request.getValue().sendAt()).isEqualTo(resumeAt);
    }

    @Test
    void usesRoomAreaForRooms() {
        when(userFilterRepository.findActiveFiltersByRoomsAndArea(2, 15f, "Минск", ListingCategory.ROOM))
            .thenReturn(List.of());

        service.notifyMatchingUsers(listing(ListingCategory.ROOM));

        verify(userFilterRepository).findActiveFiltersByRoomsAndArea(2, 15f, "Минск", ListingCategory.ROOM);
    }

    @Test
    void comparesPriceInFilterCurrency() {
        UserProfile usdInRange = user(1L);
        UserProfile usdTooCheap = user(2L);
        UserProfile bynInRange = user(3L);
        UserProfile eurRange = user(4L);
        when(userFilterRepository.findActiveFiltersByRoomsAndArea(any(), any(), any(), any())).thenReturn(List.of(
            filter(usdInRange, Currency.USD, 250, 350),
            filter(usdTooCheap, Currency.USD, 400, null),
            filter(bynInRange, Currency.BYN, null, 1000),
            filter(eurRange, Currency.EUR, 0, 1000)));

        service.notifyMatchingUsers(listing(ListingCategory.FLAT));

        verify(outboxPublisher).publish(anyString(), eq("1"), any());
        verify(outboxPublisher).publish(anyString(), eq("3"), any());
        verify(outboxPublisher, never()).publish(anyString(), eq("2"), any());
        verify(outboxPublisher, never()).publish(anyString(), eq("4"), any());
    }

    @Test
    void skipsIneligibleUsers() {
        UserProfile blocked = user(1L);
        blocked.getSettings().setRoleName(UserRole.BLOCKED);
        UserProfile botStopped = user(2L);
        botStopped.getSettings().setBotStatus(BotStatus.BLOCKED);
        UserProfile noTelegram = user(3L);
        noTelegram.setTelegramId(null);
        when(userFilterRepository.findActiveFiltersByRoomsAndArea(any(), any(), any(), any())).thenReturn(List.of(
            filter(blocked, null, null, null),
            filter(botStopped, null, null, null),
            filter(noTelegram, null, null, null)));

        service.notifyMatchingUsers(listing(ListingCategory.FLAT));

        verifyNoInteractions(outboxPublisher);
    }

    @Test
    void notifiesUserOnceForSeveralMatchingFilters() {
        UserProfile user = user(10L);
        when(userFilterRepository.findActiveFiltersByRoomsAndArea(any(), any(), any(), any()))
            .thenReturn(List.of(filter(user, null, null, null), filter(user, Currency.USD, 0, 1000)));

        service.notifyMatchingUsers(listing(ListingCategory.FLAT));

        verify(outboxPublisher, times(1)).publish(anyString(), eq("10"), any());
    }

    @Test
    void skipsListingWithoutMessage() {
        ListingCreatedEvent withoutMessage = ListingCreatedEvent.builder().listingId(7L).build();

        service.notifyMatchingUsers(withoutMessage);

        verifyNoInteractions(userFilterRepository, outboxPublisher);
    }

    private static ListingCreatedEvent listing(ListingCategory category) {
        return ListingCreatedEvent.builder()
            .listingId(7L)
            .category(category)
            .city("Минск")
            .rooms(2)
            .areaTotal(50f)
            .roomArea(15f)
            .priceUsd(new BigDecimal("300"))
            .priceByn(new BigDecimal("990"))
            .message("new flat")
            .build();
    }

    private static UserProfile user(Long id) {
        UserProfile profile = UserProfile.builder().telegramId("enc-" + id).build();
        profile.setId(id);
        UserSettings settings = UserSettings.builder().userProfile(profile).build();
        profile.setSettings(settings);
        return profile;
    }

    private static UserFilter filter(UserProfile user, Currency currency, Integer priceFrom, Integer priceTo) {
        return UserFilter.builder()
            .userProfile(user)
            .currency(currency)
            .priceFrom(priceFrom)
            .priceTo(priceTo)
            .active(true)
            .build();
    }
}
