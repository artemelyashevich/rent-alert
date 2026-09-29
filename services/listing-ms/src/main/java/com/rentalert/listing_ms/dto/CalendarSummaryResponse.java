package com.rentalert.listing_ms.dto;

import java.util.List;

public record CalendarSummaryResponse(
        String month,
        List<CalendarDaySummary> days
) {
}
