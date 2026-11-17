package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.ValidationConstants;
import com.rentalert.roommate_ms.domain.ListingStatusName;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RoommateSharedFavoriteUpdateDto(
    @NotNull(message = ValidationConstants.NOT_NULL) ListingStatusName listingStatus,
    Instant viewingDate
) {
}
