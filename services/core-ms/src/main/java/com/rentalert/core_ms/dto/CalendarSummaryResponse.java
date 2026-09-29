package com.rentalert.core_ms.dto;

import java.util.List;

public record CalendarSummaryResponse(
        String month,
        List<CalendarDaySummary> days
) {
}
