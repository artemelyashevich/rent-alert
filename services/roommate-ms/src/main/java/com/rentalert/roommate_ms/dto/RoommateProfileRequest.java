package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.ValidationConstants;
import com.rentalert.dto.currency.Currency;
import com.rentalert.roommate_ms.domain.Gender;
import com.rentalert.roommate_ms.domain.GuestsPolicy;
import com.rentalert.roommate_ms.domain.LivingSchedule;
import com.rentalert.roommate_ms.domain.PetsPolicy;
import com.rentalert.roommate_ms.domain.SmokingPolicy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.Year;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoommateProfileRequest {

    @NotNull(message = ValidationConstants.NOT_NULL)
    private Gender gender;

    private Gender[] genderPref;

    @Min(1900)
    @Max(2100)
    private Short birthYear;

    @Min(0)
    private Integer budgetMin;

    @Min(0)
    private Integer budgetMax;

    private Currency currency;

    @NotNull(message = ValidationConstants.NOT_NULL)
    @Size(max = 255)
    private String city;

    private LocalDate moveInFrom;

    private LocalDate moveInTo;

    @Size(max = 50)
    private String[] lifestyleTags;

    private LivingSchedule schedule;

    private SmokingPolicy smoking;

    private PetsPolicy pets;

    private GuestsPolicy guests;

    @Min(1)
    @Max(5)
    private Short cleanliness;

    @Size(max = 2000)
    private String bio;

    @AssertTrue(message = "birthYear must correspond to an age between 16 and 100")
    private boolean isBirthYearValid() {
        if (birthYear == null) {
            return true;
        }
        int age = Year.now().getValue() - birthYear;
        return age >= 16 && age <= 100;
    }

    @AssertTrue(message = "budgetMax must be greater than or equal to budgetMin")
    private boolean isBudgetRangeValid() {
        if (budgetMin == null || budgetMax == null) {
            return true;
        }
        return budgetMax >= budgetMin;
    }

    @AssertTrue(message = "moveInTo must be on or after moveInFrom")
    private boolean isMoveInRangeValid() {
        if (moveInFrom == null || moveInTo == null) {
            return true;
        }
        return !moveInTo.isBefore(moveInFrom);
    }
}
