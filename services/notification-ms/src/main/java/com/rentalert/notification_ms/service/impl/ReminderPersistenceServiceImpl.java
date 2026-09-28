package com.rentalert.notification_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.notification.NotificationChannelDto;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.notification.ReminderRequestDto;
import com.rentalert.dto.notification.ReminderResponseDto;
import com.rentalert.notification_ms.domain.NotificationTemplate;
import com.rentalert.notification_ms.domain.Reminder;
import com.rentalert.notification_ms.domain.ReminderStatus;
import com.rentalert.notification_ms.mapper.ReminderMapper;
import com.rentalert.notification_ms.repository.NotificationTemplateRepository;
import com.rentalert.notification_ms.repository.ReminderRepository;
import com.rentalert.notification_ms.service.ReminderPersistenceService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderPersistenceServiceImpl implements ReminderPersistenceService {

    private final ReminderRepository reminderRepository;
    private final FavoriteService favoriteService;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationSettingsService notificationSettingsService;
    private final ReminderMapper reminderMapper;

    private static final List<ReminderStatus> ACTIVE_STATUSES = List.of(ReminderStatus.NEW, ReminderStatus.PROCESSING);

    @Override
    @Transactional
    public ReminderResponseDto save(ReminderRequestDto requestDto, Long userId) {
        Favorite favorite = favoriteService.findById(userId, requestDto.listingId());

        if (favorite.getViewingDate() == null) {
            throw new AppException(InternalExceptionCode.EX400_VIEWING_DATE_REQUIRED, HttpStatus.BAD_REQUEST);
        }

        Instant remindAt = requestDto.intervalType().applyTo(favorite.getViewingDate());
        if (!remindAt.isAfter(Instant.now())) {
            throw new AppException(InternalExceptionCode.EX400_REMINDER_TIME_IN_PAST, HttpStatus.BAD_REQUEST);
        }

        if (reminderRepository.existsByUserIdAndListingIdAndIntervalTypeAndStatusIn(
                userId, requestDto.listingId(), requestDto.intervalType(), ACTIVE_STATUSES)) {
            throw new AppException(InternalExceptionCode.EX400_REMINDER_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        NotificationChannelDto channel = resolveChannel(requestDto, userId);
        NotificationTemplate notificationTemplate = notificationTemplateRepository
                .findByNotificationTypeAndEngine(NotificationType.FAVORITE_REMINDER, channel.getEngine())
                .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_NOTIFICATION_TEMPLATE_NOT_FOUND, HttpStatus.NOT_FOUND));

        Reminder reminder = Reminder.builder()
                .userId(userId)
                .listingId(requestDto.listingId())
                .remindAt(remindAt)
                .status(ReminderStatus.NEW)
                .notificationTemplate(notificationTemplate)
                .intervalType(requestDto.intervalType())
                .build();

        return reminderMapper.toResponse(reminderRepository.save(reminder));
    }

    private NotificationChannelDto resolveChannel(ReminderRequestDto requestDto, Long userId) {
        List<NotificationChannelDto> settings = notificationSettingsService.getSettings(userId);

        if (requestDto.engine() != null) {
            return settings.stream()
                    .filter(setting -> setting.getEngine() == requestDto.engine() && setting.isEnabled())
                    .findFirst()
                    .orElseThrow(() -> new AppException(InternalExceptionCode.EX400_NOTIFICATION_CHANNEL_NOT_CONFIGURED, HttpStatus.BAD_REQUEST));
        }

        return settings.stream()
                .filter(setting -> setting.isEnabled() && setting.isDefault())
                .findFirst()
                .orElseThrow(() -> new AppException(InternalExceptionCode.EX400_NOTIFICATION_CHANNEL_NOT_CONFIGURED, HttpStatus.BAD_REQUEST));
    }

    @Override
    public List<ReminderResponseDto> findAllForUser(Long userId) {
        return reminderMapper.toResponseList(reminderRepository.findAllByUserIdOrderByRemindAtAsc(userId));
    }

    @Override
    @Transactional
    public void cancel(Long reminderId, Long userId) {
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_REMINDER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (reminder.getStatus() != ReminderStatus.NEW) {
            throw new AppException(InternalExceptionCode.EX400_REMINDER_NOT_CANCELLABLE, HttpStatus.BAD_REQUEST);
        }

        reminder.setStatus(ReminderStatus.CANCELLED);
        reminderRepository.save(reminder);
    }

    @Override
    @Transactional
    public List<Reminder> findBatchToRemind(int limit) {
        List<Reminder> reminders = reminderRepository.findBatchToRemind(
                ReminderStatus.NEW.name(),
                Instant.now(),
                limit
        );
        reminders.forEach(reminder -> reminder.setStatus(ReminderStatus.PROCESSING));
        return reminderRepository.saveAll(reminders);
    }

    @Transactional
    @Override
    public void markAsSent(Reminder reminder) {
        reminder.setStatus(ReminderStatus.SENT);
        reminder.setSentAt(Instant.now());
        reminderRepository.save(reminder);
    }

    @Transactional
    @Override
    public void markAsFailed(Reminder reminder, String error) {
        reminder.setStatus(ReminderStatus.FAILED);
        reminder.setAttempts(reminder.getAttempts() + 1);
        reminder.setError(error);
        reminderRepository.save(reminder);
    }
}
