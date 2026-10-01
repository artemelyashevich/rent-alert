package com.rentalert.dto.statistics;

import com.rentalert.dto.event.user.UserEvent;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record SystemActivityOverview(
    Long totalActiveUsers,
    Long totalUsersThisWeek,
    Long totalFeedbacks,
    Map<UserEvent, Long> eventDistribution,
    List<MostActiveUser> topActiveUsers
) {

    public record MostActiveUser(Long userId, Long actionCount) {

    }
}
