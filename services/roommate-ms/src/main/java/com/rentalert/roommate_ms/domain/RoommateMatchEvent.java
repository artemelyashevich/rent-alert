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
import lombok.AllArgsConstructor;
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
@Table(name = "roommate_match_events")
public class RoommateMatchEvent extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private RoommateMatch match;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private RoommateMatchStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private RoommateMatchStatus toStatus;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "note", length = 512)
    private String note;
}
