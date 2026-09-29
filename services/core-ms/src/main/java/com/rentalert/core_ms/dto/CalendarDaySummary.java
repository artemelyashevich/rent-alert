package com.rentalert.core_ms.dto;

import java.time.LocalDate;

public record CalendarDaySummary(
        LocalDate date,
        Integer count
) {
}
