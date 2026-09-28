package com.rentalert.notification_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "notification_broadcast_recipients")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBroadcastRecipient extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", nullable = false)
    private NotificationBroadcast broadcast;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BroadcastRecipientStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
