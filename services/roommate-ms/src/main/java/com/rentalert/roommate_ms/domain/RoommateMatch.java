package com.rentalert.roommate_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roommate_matches")
public class RoommateMatch extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario", nullable = false, length = 16)
    private RoommateScenario scenario;

    @Column(name = "user_lo_id", nullable = false)
    private Long userLoId;

    @Column(name = "user_hi_id", nullable = false)
    private Long userHiId;

    @Column(name = "context_ref", nullable = false, length = 64)
    @Builder.Default
    private String contextRef = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roommate_listing_id")
    private RoommateListing roommateListing;

    @Enumerated(EnumType.STRING)
    @Column(name = "lo_decision", nullable = false, length = 16)
    @Builder.Default
    private RoommateDecision loDecision = RoommateDecision.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "hi_decision", nullable = false, length = 16)
    @Builder.Default
    private RoommateDecision hiDecision = RoommateDecision.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RoommateMatchStatus status = RoommateMatchStatus.INTERESTED;

    @Version
    @Column(name = "status_version", nullable = false)
    private Long statusVersion;

    @Column(name = "initiator_user_id", nullable = false)
    private Long initiatorUserId;

    @Column(name = "contacts_unlocked_at")
    private Instant contactsUnlockedAt;

    @Column(name = "chat_ref", length = 512)
    private String chatRef;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "lo_moved", nullable = false)
    @Builder.Default
    private boolean loMoved = false;

    @Column(name = "hi_moved", nullable = false)
    @Builder.Default
    private boolean hiMoved = false;
}
