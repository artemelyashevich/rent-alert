package com.rentalert.notification_ms.repository;

import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.domain.NotificationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByNotificationTypeAndEngine(
            NotificationType type, NotificationEngine engine);
}