package com.rentalert.notification_ms.service;

import com.rentalert.dto.notification.ReminderRequestDto;
import com.rentalert.dto.notification.ReminderResponseDto;
import com.rentalert.notification_ms.domain.Reminder;
import java.util.List;

public interface ReminderPersistenceService {

    ReminderResponseDto save(ReminderRequestDto requestDto, Long userId);

    List<ReminderResponseDto> findAllForUser(Long userId);

    void cancel(Long reminderId, Long userId);

    List<Reminder> findBatchToRemind(int limit);

    void markAsSent(Reminder reminder);

    void markAsFailed(Reminder reminder, String error);
}
