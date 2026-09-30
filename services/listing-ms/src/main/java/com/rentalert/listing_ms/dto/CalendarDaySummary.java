package com.rentalert.listing_ms.dto;

import java.time.LocalDate;

public record CalendarDaySummary(
    LocalDate date,
    Integer count
) {

}
