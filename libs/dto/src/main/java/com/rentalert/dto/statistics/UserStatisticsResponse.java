package com.rentalert.dto.statistics;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record UserStatisticsResponse(
    Long listingsReceivedToday,
    Long favoritesAddedToday,
    Long favoritesRemovedToday,
    Long filterUpdatesToday,
    Double averageListingsPerWeek,
    Long loginCountToday,
    Long profileUpdatesToday,
    Long notificationsSettingsUpdatesToday,
    Long botCommandsToday,
    Long feedbackCountToday,
    Long totalActionsToday,
    LocalDate lastActivityDate
) {

}
