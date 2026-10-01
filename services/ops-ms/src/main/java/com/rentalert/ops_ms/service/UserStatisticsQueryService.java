package com.rentalert.ops_ms.service;

import com.rentalert.dto.statistics.SystemActivityOverview;
import com.rentalert.dto.statistics.UserActionSummary;
import com.rentalert.dto.statistics.UserActivityLog;
import com.rentalert.dto.statistics.UserStatisticsResponse;
import java.time.LocalDate;
import java.util.List;

public interface UserStatisticsQueryService {

    UserStatisticsResponse getUserStatistics(Long userId, Long filterId);

    UserActionSummary getUserActionSummary(Long userId, LocalDate since);

    List<UserActionSummary> getTopActiveUsers(int limit, LocalDate since);

    List<UserActivityLog> getUserActivityLog(Long userId, LocalDate since, LocalDate until);

    SystemActivityOverview getSystemActivityOverview(LocalDate since);
}
