package com.rentalert.listing_ms.dto;

import com.rentalert.dto.ValidationConstants;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record FavoriteCreateDto(
        @NotNull(message = ValidationConstants.NOT_NULL) Long listingId,
        @NotNull(message = ValidationConstants.NOT_NULL) Long listingStatusId,
        Instant viewingDate
) {
}
