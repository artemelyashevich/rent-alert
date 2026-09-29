package com.rentalert.listing_ms.dto;

import java.time.LocalDate;

public record CalendarDaySummaryDto(
        LocalDate date,
        int count
) {
}