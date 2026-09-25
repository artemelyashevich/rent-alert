package com.rentalert.identity_ms.dto;


import com.rentalert.identity_ms.domain.FeedbackReasonCode;
import com.rentalert.identity_ms.domain.FeedbackSubjectType;
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
