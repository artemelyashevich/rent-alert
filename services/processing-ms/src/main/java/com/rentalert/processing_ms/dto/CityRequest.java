package com.rentalert.processing_ms.dto;

import com.rentalert.dto.listing.ScheduleProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CityRequest {

    @NotNull(message = "Провайдер обязателен")
    private ScheduleProvider provider;

    @NotBlank(message = "Название города обязательно")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Код города у провайдера обязателен")
    @Size(max = 255)
    private String cityCode;

    @Builder.Default
    private boolean active = true;
}
