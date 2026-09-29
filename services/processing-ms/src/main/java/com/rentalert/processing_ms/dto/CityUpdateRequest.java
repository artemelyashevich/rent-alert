package com.rentalert.processing_ms.dto;

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
public class CityUpdateRequest {

    @NotBlank(message = "Название города обязательно")
    @Size(max = 255)
    private String name;

    @NotNull(message = "Флаг активности обязателен")
    private Boolean active;
}
