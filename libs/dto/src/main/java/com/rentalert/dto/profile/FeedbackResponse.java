package com.rentalert.dto.profile;


import java.time.Instant;

public record FeedbackResponse(
    Long id,
    Long userId,
    String username,
    String message,
    Integer rating,
    FeedbackSubjectType subjectType,
    Long subjectId,
    FeedbackReasonCode reasonCode,
    Instant createdAt,
    Instant updatedAt
) {

}
