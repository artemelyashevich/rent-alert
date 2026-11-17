package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RoommateListingResponse(
    Long id,
    Long authorUserId,
    Long listingId,
    String city,
    String addressExact,
    String district,
    String metroStation,
    BigDecimal rentShare,
    Currency currency,
    Integer roomCount,
    Integer flatmatesCount,
    LocalDate availableFrom,
    String[] houseRules,
    String descriptionSubmitted,
    String descriptionApproved,
    ModerationStatus descriptionModStatus,
    RoommateListingStatus status,
    String[] genderPref,
    Integer ageMin,
    Integer ageMax,
    Instant createdAt,
    Instant updatedAt
) {

}
