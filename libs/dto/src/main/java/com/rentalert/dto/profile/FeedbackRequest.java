package com.rentalert.dto.profile;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class FeedbackRequest {

    private String message;

    @Min(1)
    @Max(5)
    private Integer rating;

    @Builder.Default
    private FeedbackSubjectType subjectType = FeedbackSubjectType.APP;

    private Long subjectId;

    private FeedbackReasonCode reasonCode;

    @AssertTrue(message = "Оценка обязательна для отзыва о приложении")
    private boolean isRatingProvided() {
        return subjectType != FeedbackSubjectType.APP || rating != null;
    }

    @AssertTrue(message = "Для жалобы укажите subjectId и reasonCode")
    private boolean isComplaintSubjectValid() {
        return subjectType == FeedbackSubjectType.APP || (subjectId != null && reasonCode != null);
    }
}