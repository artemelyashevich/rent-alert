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
    name = "notification_templates",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_templates_type_engine",
        columnNames = {"notification_type", "engine"}
    )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine", nullable = false, length = 50)
    private NotificationEngine engine;

    @Column(name = "template_text", nullable = false, columnDefinition = "TEXT")
    private String templateText;

    @Column(name = "template_name", nullable = true, columnDefinition = "TEXT")
    private String templateName;
}