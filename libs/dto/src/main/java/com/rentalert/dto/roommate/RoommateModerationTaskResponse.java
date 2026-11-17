package com.rentalert.dto.roommate;

import java.time.Instant;

public record RoommateModerationTaskResponse(
    Long id,
    ModerationSubjectType subjectType,
    Long subjectId,
    String field,
    String submittedText,
    ModerationStatus status,
    Long reviewerId,
    String decisionReason,
    Instant createdAt
) {

}
