package com.rentalert.notification_ms.service;


import com.rentalert.dto.notification.NotificationDto;
import com.rentalert.dto.notification.NotificationEngine;

public interface NotificationStrategy {

    void sendNotification(NotificationDto notificationDto);

    NotificationEngine getSupportedEngine();
}
