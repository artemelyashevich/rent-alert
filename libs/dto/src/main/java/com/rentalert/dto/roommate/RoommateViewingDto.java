package com.rentalert.dto.roommate;

import java.time.Instant;

public record RoommateViewingDto(
    Long id,
    Long matchId,
    Long userLoId,
    Long userHiId,
    Instant viewingDate,
    RoommateViewingStatus status,
    Long listingId,
    String address,
    String listingTitle,
    String listingStreet
) {

}
