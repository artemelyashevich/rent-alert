package com.rentalert.dto.profile.filter;

import com.rentalert.dto.AbstractDto;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.listing.ListingCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
public class UserFilterRequest extends AbstractDto {

    /** NULL — фильтр действует для обеих категорий (квартиры и комнаты). */
    private ListingCategory category;

    @Min(0)
    private Integer priceFrom;

    @Min(0)
    private Integer priceTo;

    @Min(1)
    @Max(10)
    private Integer roomsFrom;

    @Min(1)
    @Max(10)
    private Integer roomsTo;

    @Min(0)
    private Float areaFrom;

    @Min(0)
    private Float areaTo;

    @Builder.Default
    private Currency currency = Currency.USD;

    @Size(max = 255)
    @Builder.Default
    private String city = "Минск";

    @AssertTrue(message = "priceTo must be greater than priceFrom")
    private boolean isPriceToGreaterThanPriceFrom() {
        if (priceFrom == null || priceTo == null) return true;
        return priceTo > priceFrom;
    }

    @AssertTrue(message = "roomsTo must be greater than roomsFrom")
    private boolean isRoomsToGreaterThanRoomsFrom() {
        if (roomsFrom == null || roomsTo == null) return true;
        return roomsTo >= roomsFrom;
    }

    @AssertTrue(message = "areaTo must be greater than areaFrom")
    private boolean isAreaToGreaterThanAreaFrom() {
        if (areaFrom == null || areaTo == null) return true;
        return areaTo > areaFrom;
    }
}