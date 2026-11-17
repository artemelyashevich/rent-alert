package com.rentalert.roommate_ms.dto;

import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import jakarta.validation.constraints.NotNull;

public record RoommateListingStatusRequest(@NotNull RoommateListingStatus status) {

}
