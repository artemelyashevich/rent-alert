package com.rentalert.listing_ms.dto;

import com.rentalert.dto.currency.Currency;
import com.rentalert.listing_ms.domain.ListingStatusName;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

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
