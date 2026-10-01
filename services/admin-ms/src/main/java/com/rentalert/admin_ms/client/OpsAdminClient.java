package com.rentalert.admin_ms.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.audit.AuditLogResponse;
import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.job.JobSettingResponse;
import com.rentalert.dto.statistics.SystemActivityOverview;
import com.rentalert.dto.statistics.UserActionSummary;
import com.rentalert.dto.statistics.UserActivityLog;
import com.rentalert.dto.statistics.UserStatisticsResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ops-ms", contextId = "opsAdminClient", path = "/ops/internal/admin")
public interface OpsAdminClient {

    @GetMapping("/audit-logs")
    PageResponse<AuditLogResponse> findAuditLogs(
        @RequestParam(value = "eventInitiator", required = false) EventInitiatorType eventInitiator,
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @DeleteMapping("/audit-logs")
    int cleanUpAuditLogs(@RequestParam("retentionDays") int retentionDays);

    @GetMapping("/job-settings")
    PageResponse<JobSettingResponse> findJobSettings(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @PostMapping("/jobs/cleanup")
    UUID startCleanup();

    @GetMapping("/user-statistics/{userId}")
    UserStatisticsResponse getUserStatistics(@PathVariable("userId") Long userId,
        @RequestParam(value = "filterId", required = false) Long filterId);

    @GetMapping("/user-statistics/actions")
    List<UserActionSummary> getUserActionSummary(
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam("since") String since,
        @RequestParam("limit") int limit
    );

    @GetMapping("/user-statistics/actions/{userId}/log")
    List<UserActivityLog> getUserActivityLog(
        @PathVariable("userId") Long userId,
        @RequestParam("since") String since,
        @RequestParam(value = "until", required = false) String until
    );

    @GetMapping("/user-statistics/system")
    SystemActivityOverview getSystemActivity(@RequestParam("since") String since);
}
