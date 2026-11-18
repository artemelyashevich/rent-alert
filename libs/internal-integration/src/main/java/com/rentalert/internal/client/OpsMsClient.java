package com.rentalert.internal.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.audit.AuditLogResponse;
import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.job.JobSettingResponse;
import com.rentalert.dto.migration.MigrationRunResponse;
import com.rentalert.dto.migration.MigrationStartRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = OpsMsClient.SERVICE_NAME, path = "/ops/internal")
public interface OpsMsClient {

    String SERVICE_NAME = "ops-ms";

    @GetMapping("/admin/audit-logs")
    PageResponse<AuditLogResponse> findAuditLogs(
        @RequestParam(value = "eventInitiator", required = false) EventInitiatorType eventInitiator,
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @DeleteMapping("/admin/audit-logs")
    int cleanUpAuditLogs(@RequestParam("retentionDays") int retentionDays);

    @GetMapping("/admin/job-settings")
    PageResponse<JobSettingResponse> findJobSettings(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @PostMapping("/admin/jobs/cleanup")
    UUID startCleanup();

    @GetMapping("/admin/user-statistics/{userId}")
    UserStatisticsResponse getUserStatistics(@PathVariable("userId") Long userId,
        @RequestParam(value = "filterId", required = false) Long filterId);

    @GetMapping("/admin/user-statistics/actions")
    List<UserActionSummary> getUserActionSummary(
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam("since") String since,
        @RequestParam("limit") int limit
    );

    @GetMapping("/admin/user-statistics/actions/{userId}/log")
    List<UserActivityLog> getUserActivityLog(
        @PathVariable("userId") Long userId,
        @RequestParam("since") String since,
        @RequestParam(value = "until", required = false) String until
    );

    @GetMapping("/admin/user-statistics/system")
    SystemActivityOverview getSystemActivity(@RequestParam("since") String since);

    @PostMapping("/admin/migrations")
    MigrationRunResponse startMigration(@RequestBody MigrationStartRequest request,
        @RequestParam("requestedBy") Long requestedBy);

    @GetMapping("/admin/migrations")
    PageResponse<MigrationRunResponse> listMigrations(@RequestParam("size") int size);

    @GetMapping("/admin/migrations/{id}")
    MigrationRunResponse getMigration(@PathVariable("id") UUID id);

    @PostMapping("/admin/migrations/{id}/cancel")
    MigrationRunResponse cancelMigration(@PathVariable("id") UUID id);
}
