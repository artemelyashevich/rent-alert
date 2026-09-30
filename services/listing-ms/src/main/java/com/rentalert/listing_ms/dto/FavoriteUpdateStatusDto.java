package com.rentalert.listing_ms.dto;

import com.rentalert.dto.ValidationConstants;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record FavoriteUpdateStatusDto(
    @NotNull(message = ValidationConstants.NOT_NULL) Long listingStatusId,
    Instant viewingDate
) {

}
