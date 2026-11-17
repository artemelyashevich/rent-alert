package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.currency.Currency;
import com.rentalert.roommate_ms.domain.RoommateViewingStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record RoommateCalendarEventResponse(
    Long matchId,
    Long listingId,
    String title,
    String address,
    String metroStation,
    BigDecimal price,
    Currency currency,
    String coverPhotoUrl,
    Instant viewingDate,
    RoommateViewingStatus status,
    Long counterpartyUserId,
    String counterpartyUsername
) {
}
