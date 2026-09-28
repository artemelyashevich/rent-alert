package com.rentalert.notification_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "notification_broadcasts")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBroadcast extends AbstractEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "total_recipients", nullable = false)
    @Builder.Default
    private int totalRecipients = 0;

    @Column(name = "sent_count", nullable = false)
    @Builder.Default
    private int sentCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private int failedCount = 0;

    @Column(name = "skipped_count", nullable = false)
    @Builder.Default
    private int skippedCount = 0;

    @Column(name = "pinned", nullable = false)
    @Builder.Default
    private boolean pinned = false;

    @OneToMany(mappedBy = "broadcast", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NotificationBroadcastRecipient> recipients = new ArrayList<>();
}
