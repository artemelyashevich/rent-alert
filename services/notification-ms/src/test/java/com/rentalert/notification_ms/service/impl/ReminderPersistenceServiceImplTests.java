package com.rentalert.notification_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.listing.FavoriteViewingDto;
import com.rentalert.dto.notification.NotificationChannelDto;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.notification.ReminderIntervalType;
import com.rentalert.dto.notification.ReminderRequestDto;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.notification_ms.domain.NotificationTemplate;
import com.rentalert.notification_ms.domain.Reminder;
import com.rentalert.notification_ms.mapper.ReminderMapper;
import com.rentalert.notification_ms.repository.NotificationTemplateRepository;
import com.rentalert.notification_ms.repository.ReminderRepository;
import com.rentalert.notification_ms.service.NotificationSettingsService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReminderPersistenceServiceImplTests {

    private final ReminderRepository reminderRepository = mock(ReminderRepository.class);
    private final NotificationTemplateRepository templateRepository = mock(NotificationTemplateRepository.class);
    private final NotificationSettingsService settingsService = mock(NotificationSettingsService.class);
    private final ListingMsService listingMsService = mock(ListingMsService.class);
    private final ReminderPersistenceServiceImpl service = new ReminderPersistenceServiceImpl(
        reminderRepository, templateRepository, settingsService, mock(ReminderMapper.class), listingMsService);

    private final ReminderRequestDto request = new ReminderRequestDto(ReminderIntervalType.BEFORE_1_DAY, null, 15L);

    @Test
    void missingFavoriteIsNotFound() {
        when(listingMsService.getFavoriteViewing(42L, 15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(request, 42L))
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(InternalExceptionCode.EX404_FAVORITE_NOT_FOUND);
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void favoriteWithoutViewingDateIsRejected() {
        when(listingMsService.getFavoriteViewing(42L, 15L)).thenReturn(Optional.of(new FavoriteViewingDto(42L, 15L, null, null)));

        assertThatThrownBy(() -> service.save(request, 42L))
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(InternalExceptionCode.EX400_VIEWING_DATE_REQUIRED);
    }

    @Test
    void remindsBeforeViewingDateOnDefaultChannel() {
        Instant viewingDate = Instant.now().plus(3, ChronoUnit.DAYS);
        NotificationTemplate template = NotificationTemplate.builder()
            .notificationType(NotificationType.FAVORITE_REMINDER)
            .engine(NotificationEngine.TELEGRAM_BOT)
            .build();
        when(listingMsService.getFavoriteViewing(42L, 15L))
            .thenReturn(Optional.of(new FavoriteViewingDto(42L, 15L, viewingDate, null)));
        when(settingsService.getSettings(42L))
            .thenReturn(List.of(new NotificationChannelDto(NotificationEngine.TELEGRAM_BOT, true, true, true)));
        when(templateRepository.findByNotificationTypeAndEngine(NotificationType.FAVORITE_REMINDER, NotificationEngine.TELEGRAM_BOT))
            .thenReturn(Optional.of(template));
        when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(request, 42L);

        ArgumentCaptor<Reminder> saved = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(saved.capture());
        assertThat(saved.getValue().getRemindAt()).isEqualTo(viewingDate.minus(1, ChronoUnit.DAYS));
        assertThat(saved.getValue().getNotificationTemplate()).isSameAs(template);
    }
}
