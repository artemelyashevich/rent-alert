package com.rentalert.dto.roommate;

import java.time.Instant;

public record RoommateMatchParticipantsDto(
    Long matchId,
    RoommateMatchStatus status,
    Long userLoId,
    Long userHiId,
    Instant contactsUnlockedAt
) {

}
