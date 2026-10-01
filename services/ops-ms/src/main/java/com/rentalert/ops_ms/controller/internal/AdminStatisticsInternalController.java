package com.rentalert.ops_ms.controller.internal;

import com.rentalert.dto.statistics.SystemActivityOverview;
import com.rentalert.dto.statistics.UserActionSummary;
import com.rentalert.dto.statistics.UserActivityLog;
import com.rentalert.dto.statistics.UserStatisticsResponse;
import com.rentalert.ops_ms.configuration.PublicUrls;
import com.rentalert.ops_ms.service.UserStatisticsQueryService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG + "/user-statistics")
@RequiredArgsConstructor
public class AdminStatisticsInternalController {

    private final UserStatisticsQueryService userStatisticsQueryService;

    @GetMapping("/{userId}")
    public UserStatisticsResponse getUserStatistics(@PathVariable Long userId, @RequestParam(required = false) Long filterId) {
        return userStatisticsQueryService.getUserStatistics(userId, filterId);
    }

    @GetMapping("/actions")
    public List<UserActionSummary> getUserActionSummary(
        @RequestParam(required = false) Long userId,
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate since,
        @RequestParam(defaultValue = "10") int limit
    ) {
        if (userId != null) {
            return List.of(userStatisticsQueryService.getUserActionSummary(userId, since));
        }
        return userStatisticsQueryService.getTopActiveUsers(limit, since);
    }

    @GetMapping("/actions/{userId}/log")
    public List<UserActivityLog> getUserActivityLog(
        @PathVariable Long userId,
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate since,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate until
    ) {
        return userStatisticsQueryService.getUserActivityLog(userId, since, until);
    }

    @GetMapping("/system")
    public SystemActivityOverview getSystemActivity(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate since) {
        return userStatisticsQueryService.getSystemActivityOverview(since);
    }
}
