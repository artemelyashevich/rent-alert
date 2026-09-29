package com.rentalert.core_ms.dto;

import com.rentalert.dto.AbstractDto;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class SchedulerSettingsRequest extends AbstractDto {

    @NotNull
    private Boolean schedulerEnabled;

    @NotNull
    private Boolean notificationsEnabled;

    @NotNull
    @Min(value = 10000, message = "Минимальный интервал — 10 секунд (10000 мс)")
    private Long fixedRateMs;

    @NotNull
    @Min(1)
    @Max(500)
    private Integer pageSize;

    @Min(0)
    private Integer priceFrom;

    @Min(0)
    private Integer priceTo;

    @Size(max = 10)
    @Pattern(regexp = "^(USD|BYN)$", message = "priceCurrency must be either USD or BYN")
    private String priceCurrency;

    @NotBlank
    @Size(max = 255)
    private String regionId;

    @NotBlank
    @Size(max = 50)
    private String sortBy;

    @NotBlank
    @Size(max = 10)
    @Pattern(regexp = "^(ASC|DESC)$", message = "sortOrder must be either ASC or DESC")
    private String sortOrder;

    @NotNull
    private Integer categoryId;

    private Integer roomCategoryId;

    @NotBlank
    @Pattern(regexp = "^(KUFAR|REALT|ONLINER)$", message = "provider must be either KUFAR or REALT")
    private String providerName;

    @AssertTrue(message = "priceTo must be greater than priceFrom")
    private boolean isPriceToGreaterThanPriceFrom() {
        if (priceFrom == null || priceTo == null) {
            return true;
        }
        return priceTo > priceFrom;
    }
}