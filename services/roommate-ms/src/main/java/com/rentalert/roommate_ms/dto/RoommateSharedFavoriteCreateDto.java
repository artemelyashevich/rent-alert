package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.ValidationConstants;
import com.rentalert.roommate_ms.domain.ListingStatusName;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RoommateSharedFavoriteCreateDto(
    @NotNull(message = ValidationConstants.NOT_NULL) Long listingId,
    @NotNull(message = ValidationConstants.NOT_NULL) ListingStatusName listingStatus,
    Instant viewingDate
) {
}
