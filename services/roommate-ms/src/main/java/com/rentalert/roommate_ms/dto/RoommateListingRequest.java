package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.ValidationConstants;
import com.rentalert.dto.currency.Currency;
import com.rentalert.roommate_ms.domain.Gender;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class RoommateListingRequest {

    private Long listingId;

    @NotNull(message = ValidationConstants.NOT_NULL)
    @Size(max = 255)
    private String city;

    @Size(max = 512)
    private String addressExact;

    @Size(max = 255)
    private String district;

    @Size(max = 255)
    private String metroStation;

    @Min(0)
    private BigDecimal rentShare;

    private Currency currency;

    @Min(1)
    private Integer roomCount;

    @Min(1)
    private Integer flatmatesCount;

    private LocalDate availableFrom;

    @Size(max = 50)
    private String[] houseRules;

    @Size(max = 2000)
    private String description;

    private Gender[] genderPref;

    @Min(16)
    private Integer ageMin;

    @Min(16)
    private Integer ageMax;

    @AssertTrue(message = "ageMax must be greater than or equal to ageMin")
    private boolean isAgeRangeValid() {
        if (ageMin == null || ageMax == null) {
            return true;
        }
        return ageMax >= ageMin;
    }
}
