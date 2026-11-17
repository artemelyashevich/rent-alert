package com.rentalert.roommate_ms.dto;

import jakarta.validation.constraints.NotNull;

public record RoommateVisibilityRequest(@NotNull Boolean searchable) {

}
