package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.listing.ListingDto;
import com.rentalert.roommate_ms.domain.ListingStatusName;
import java.time.Instant;

public record RoommateSharedFavoriteResponse(
    ListingDto listing,
    ListingStatusName status,
    Instant viewingDate,
    Long addedByUserId,
    Instant createdAt,
    Instant updatedAt
) {
}
