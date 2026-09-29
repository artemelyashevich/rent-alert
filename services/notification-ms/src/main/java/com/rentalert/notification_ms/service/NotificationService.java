package com.rentalert.notification_ms.service;


import com.rentalert.dto.notification.NotificationDispatchResult;
import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.notification_ms.domain.Notification;
import java.time.Instant;
import java.util.List;

public interface NotificationService {

    List<NotificationDispatchResult> sendNotification(NotificationDto notificationDto);

    void scheduleNotification(NotificationDto notificationDto, Instant sendAt);

    void retryFailed();

    List<Notification> enqueue(NotificationRequest request, Instant sendAt);

    void dispatch(List<Notification> notifications);

    /** Уведомления в терминальном статусе старше {@code cutoff}. */
    int deleteObsoleteNotifications(Instant cutoff);

    /** Уведомления, привязанные к указанным объявлениям (используется при удалении объявлений). */
    int deleteByListingIds(List<Long> listingIds);
}
