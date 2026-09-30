package com.rentalert.listing_ms.dto;

import com.rentalert.dto.listing.ListingDto;
import com.rentalert.listing_ms.domain.ListingStatusName;
import java.time.Instant;

public record FavoriteResponse(
    ListingDto listing,
    ListingStatusInfo status,
    Instant viewingDate,
    Instant createdAt,
    Instant updatedAt
) {

    public record ListingStatusInfo(
        Long id,
        ListingStatusName name,
        String color,
        String description
    ) {

    }
}
