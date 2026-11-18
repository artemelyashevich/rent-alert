package com.rentalert.dto.event.roommate;

import java.util.UUID;

public record RoommateComplaintEvent(UUID eventId, SubjectType subjectType, Long subjectId) {

    public enum SubjectType {
        ROOMMATE_PROFILE, ROOMMATE_LISTING
    }
}
