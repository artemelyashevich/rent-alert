package com.rentalert.dto.event.roommate;

import com.rentalert.dto.event.KafkaTopics;
import java.util.UUID;

/**
 * Complaint about a roommate profile or roommate listing, sent by identity-ms to {@link KafkaTopics#ROOMMATE_COMPLAINTS}.
 * {@code eventId} is the idempotency key for consumers; {@code subjectId} is the profile owner's user id or the roommate listing id depending on {@code subjectType}.
 */
public record RoommateComplaintEvent(UUID eventId, SubjectType subjectType, Long subjectId) {

    public enum SubjectType {
        ROOMMATE_PROFILE, ROOMMATE_LISTING
    }
}
