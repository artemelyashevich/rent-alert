package com.rentalert.dto.statistics;

import java.time.LocalDate;
import lombok.Builder;

@Builder(toBuilder = true)
public record UserActionSummary(
    Long userId,
    String username,
    LocalDate lastActivityDate,
    Long totalActions,
    Long loginCount,
    Long favoriteActions,
    Long filterActions,
    Long botCommandCount,
    Long feedbackCount,
    Long callbackCount,
    Double actionRatePerDay
) {

}
