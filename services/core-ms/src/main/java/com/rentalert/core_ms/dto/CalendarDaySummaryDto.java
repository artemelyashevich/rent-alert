package com.rentalert.core_ms.dto;

import java.time.LocalDate;

public record CalendarDaySummaryDto(
        LocalDate date,
        int count
) {
}