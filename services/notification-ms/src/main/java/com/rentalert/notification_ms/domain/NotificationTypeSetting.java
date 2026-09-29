package com.rentalert.notification_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * How a notification type is delivered; editable without a release.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "notification_type_settings")
public class NotificationTypeSetting extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, unique = true, length = 50)
    private NotificationType notificationType;

    /** Grouping shown to clients, e.g. in the realtime payload. */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Convert(converter = NotificationEnginesConverter.class)
    @Column(name = "engines", nullable = false)
    private List<NotificationEngine> engines;

    /** Sent even on channels the user switched off (e.g. system messages). */
    @Column(name = "mandatory", nullable = false)
    private boolean mandatory;

    /** Kill switch: a disabled type is accepted but not delivered. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;
}
