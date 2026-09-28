package com.rentalert.identity_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.profile.FeedbackReasonCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "feedbacks")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserProfile userProfile;

    @Column(name = "message", length = 2000)
    private String message;

    @Min(1)
    @Max(5)
    @Column(name = "rating")
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    @Builder.Default
    private com.rentalert.dto.profile.FeedbackSubjectType subjectType = com.rentalert.dto.profile.FeedbackSubjectType.APP;

    @Column(name = "subject_id")
    private Long subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 20)
    private FeedbackReasonCode reasonCode;
}
