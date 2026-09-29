package com.rentalert.core_ms.dto;

import com.rentalert.core_ms.domain.ListingStatusName;
import com.rentalert.dto.listing.ListingDto;
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
