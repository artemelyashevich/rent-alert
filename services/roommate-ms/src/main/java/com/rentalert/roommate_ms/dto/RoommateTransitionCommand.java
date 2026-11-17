package com.rentalert.roommate_ms.dto;

import java.time.Instant;

public record RoommateTransitionCommand(Long listingId, String address, Instant viewingDate) {

    public static RoommateTransitionCommand empty() {
        return new RoommateTransitionCommand(null, null, null);
    }
}
