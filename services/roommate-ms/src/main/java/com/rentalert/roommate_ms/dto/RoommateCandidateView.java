package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.currency.Currency;
import com.rentalert.roommate_ms.domain.GuestsPolicy;
import com.rentalert.roommate_ms.domain.LivingSchedule;
import com.rentalert.roommate_ms.domain.PetsPolicy;
import com.rentalert.roommate_ms.domain.SmokingPolicy;
import java.time.LocalDate;
import java.util.List;

public record RoommateCandidateView(
    Long userId,
    String username,
    String photoUrl,
    String ageRange,
    String city,
    Integer budgetMin,
    Integer budgetMax,
    Currency currency,
    LocalDate moveInFrom,
    LocalDate moveInTo,
    List<String> lifestyleTags,
    List<String> sharedTags,
    LivingSchedule schedule,
    SmokingPolicy smoking,
    PetsPolicy pets,
    GuestsPolicy guests,
    String bio,
    int score
) {

}
