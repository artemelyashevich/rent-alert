package com.rentalert.notification_ms.service.impl;

import com.rentalert.notification_ms.repository.NotificationBroadcastRecipientRepository;
import com.rentalert.notification_ms.repository.NotificationRepository;
import com.rentalert.notification_ms.repository.ReminderRepository;
import com.rentalert.notification_ms.repository.TelegramMessageRepository;
import com.rentalert.notification_ms.repository.UserNotificationSettingRepository;
import com.rentalert.notification_ms.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataServiceImpl implements UserDataService {

    private final ReminderRepository reminderRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final NotificationBroadcastRecipientRepository broadcastRecipientRepository;
    private final TelegramMessageRepository telegramMessageRepository;

    @Override
    @Transactional
    public void deleteUserData(Long userId) {
        int reminders = reminderRepository.deleteByUserId(userId);
        int notifications = notificationRepository.deleteByUserId(userId);
        int recipients = broadcastRecipientRepository.deleteByUserId(userId);
        int messages = telegramMessageRepository.deleteByUserId(userId);
        userNotificationSettingRepository.deleteByUserId(userId);
        log.info("Deleted data of user {}: reminders={}, notifications={}, broadcastRecipients={}, telegramMessages={}",
            userId, reminders, notifications, recipients, messages);
    }
}
