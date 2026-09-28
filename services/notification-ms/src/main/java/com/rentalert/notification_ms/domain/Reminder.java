package com.rentalert.notification_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.notification.ReminderIntervalType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reminders")
public class Reminder extends AbstractEntity {
    private Long userId;

    private Long listingId;

    private Long roommateViewingId;

    private Instant remindAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ReminderStatus status = ReminderStatus.NEW;

    @ManyToOne
    @JoinColumn(name = "notification_template_id")
    private NotificationTemplate notificationTemplate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ReminderIntervalType intervalType = ReminderIntervalType.BEFORE_1_DAY;

    private Instant sentAt;

    @Builder.Default
    private Integer attempts = 0;

    private String error;
}