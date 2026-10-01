package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.client.OpsAdminClient;
import com.rentalert.admin_ms.client.UserBriefs;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.statistics.SystemActivityOverview;
import com.rentalert.dto.statistics.UserActionSummary;
import com.rentalert.dto.statistics.UserActivityLog;
import com.rentalert.dto.statistics.UserStatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin user statistics", description = "Статистика действий пользователей")
@RestController
@RequestMapping(PublicUrls.USER_STATISTICS_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final OpsAdminClient opsAdminClient;
    private final UserBriefs userBriefs;

    @Operation(summary = "Статистика пользователя за сегодня")
    @GetMapping("/{userId}")
    public UserStatisticsResponse getUserStatistics(@PathVariable Long userId, @RequestParam(required = false) Long filterId) {
        return opsAdminClient.getUserStatistics(userId, filterId);
    }

    @Operation(summary = "Сводка действий пользователя или топ активных пользователей, если userId не указан")
    @GetMapping("/actions")
    public List<UserActionSummary> getUserActionSummary(
        @RequestParam(required = false) Long userId,
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate since,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return withUsernames(opsAdminClient.getUserActionSummary(userId, since.toString(), limit));
    }

    @Operation(summary = "Журнал активности пользователя по дням")
    @GetMapping("/actions/{userId}/log")
    public List<UserActivityLog> getUserActivityLog(
        @PathVariable Long userId,
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate since,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate until
    ) {
        return opsAdminClient.getUserActivityLog(userId, since.toString(), until == null ? null : until.toString());
    }

    @Operation(summary = "Обзор активности системы")
    @GetMapping("/system")
    public SystemActivityOverview getSystemActivity(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate since) {
        return opsAdminClient.getSystemActivity(since.toString());
    }

    private List<UserActionSummary> withUsernames(List<UserActionSummary> summaries) {
        Map<Long, UserBriefResponse> briefs = userBriefs.byId(summaries.stream().map(UserActionSummary::userId).toList());
        return summaries.stream()
            .map(summary -> {
                UserBriefResponse brief = briefs.get(summary.userId());
                return brief == null ? summary : summary.toBuilder().username(brief.username()).build();
            })
            .toList();
    }
}
