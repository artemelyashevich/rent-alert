package com.rentalert.notification_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationEngine engine;

    @Column(nullable = false)
    private String receiver;

    @Column(name = "user_id")
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private int attempt = 1;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "deduplication_key")
    private String deduplicationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_type")
    private NotificationType retryType;

    @Column(name = "template_params_json", columnDefinition = "TEXT")
    private String templateParamsJson;

    @Column(name = "listing_id")
    private Long listingId;

    @Column(name = "request_id")
    private UUID requestId;
}