package com.rentalert.roommate_ms.dto;

import java.time.Instant;

public record RoommateTransitionRequest(Long listingId, String address, Instant viewingDate, Long expectedVersion) {

    public RoommateTransitionCommand toCommand() {
        return new RoommateTransitionCommand(listingId, address, viewingDate);
    }
}
