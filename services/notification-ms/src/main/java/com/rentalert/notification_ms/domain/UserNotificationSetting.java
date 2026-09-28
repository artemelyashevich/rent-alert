package com.rentalert.notification_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.notification.NotificationEngine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "user_notification_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_engine",
                columnNames = {"user_id", "engine"}
        )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationSetting extends AbstractEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine", nullable = false, length = 50)
    private NotificationEngine engine;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;
    
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;
}