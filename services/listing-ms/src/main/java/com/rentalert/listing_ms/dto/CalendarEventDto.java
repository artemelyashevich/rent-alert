package com.rentalert.listing_ms.dto;

import com.rentalert.listing_ms.domain.ListingStatusName;
import com.rentalert.dto.currency.Currency;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record CalendarEventDto(
        Long listingId,
        String title,
        String address,
        BigDecimal price,
        Currency currency,
        String coverPhotoUrl,
        Instant viewingDate,
        ListingStatusName status
) {
}
