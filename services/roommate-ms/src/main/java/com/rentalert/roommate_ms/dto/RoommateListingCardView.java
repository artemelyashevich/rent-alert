package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.currency.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RoommateListingCardView(
    Long id,
    Long authorUserId,
    String authorUsername,
    String authorPhotoUrl,
    String city,
    String district,
    String metroStation,
    String addressPublic,
    Double geoLat,
    Double geoLng,
    BigDecimal rentShare,
    Currency currency,
    Integer roomCount,
    Integer flatmatesCount,
    LocalDate availableFrom,
    List<String> houseRules,
    String description,
    List<String> genderPref,
    Integer ageMin,
    Integer ageMax,
    int score
) {

}
