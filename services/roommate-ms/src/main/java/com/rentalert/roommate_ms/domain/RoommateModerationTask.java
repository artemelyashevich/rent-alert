package com.rentalert.roommate_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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
@Table(name = "roommate_moderation_tasks")
public class RoommateModerationTask extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 16)
    private ModerationSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "field", nullable = false, length = 64)
    private String field;

    @Column(name = "submitted_text", columnDefinition = "TEXT")
    private String submittedText;

    @Column(name = "text_hash", length = 64)
    private String textHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ModerationStatus status = ModerationStatus.PENDING_MANUAL;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "decision_reason", length = 512)
    private String decisionReason;
}
