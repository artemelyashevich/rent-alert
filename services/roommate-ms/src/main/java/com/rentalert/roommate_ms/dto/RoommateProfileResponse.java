package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.roommate_ms.domain.Gender;
import com.rentalert.roommate_ms.domain.GuestsPolicy;
import com.rentalert.roommate_ms.domain.LivingSchedule;
import com.rentalert.roommate_ms.domain.PetsPolicy;
import com.rentalert.roommate_ms.domain.SmokingPolicy;
import java.time.Instant;
import java.time.LocalDate;

public record RoommateProfileResponse(
    Long userId,
    Gender gender,
    Gender[] genderPref,
    Short birthYear,
    Integer budgetMin,
    Integer budgetMax,
    Currency currency,
    String city,
    LocalDate moveInFrom,
    LocalDate moveInTo,
    String[] lifestyleTags,
    LivingSchedule schedule,
    SmokingPolicy smoking,
    PetsPolicy pets,
    GuestsPolicy guests,
    Short cleanliness,
    String bioSubmitted,
    String bioApproved,
    ModerationStatus bioModStatus,
    boolean active,
    boolean searchable,
    Instant createdAt,
    Instant updatedAt
) {

}
