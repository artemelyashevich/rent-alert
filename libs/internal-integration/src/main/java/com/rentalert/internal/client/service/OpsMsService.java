package com.rentalert.internal.client.service;

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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface OpsMsService {

    PageResponse<AuditLogResponse> findAuditLogs(EventInitiatorType eventInitiator, Long userId, Pageable pageable);

    int cleanUpAuditLogs(int retentionDays);

    PageResponse<JobSettingResponse> findJobSettings(Pageable pageable);

    UUID startCleanup();

    UserStatisticsResponse getUserStatistics(Long userId, Long filterId);

    List<UserActionSummary> getUserActionSummary(Long userId, LocalDate since, int limit);

    List<UserActivityLog> getUserActivityLog(Long userId, LocalDate since, LocalDate until);

    SystemActivityOverview getSystemActivity(LocalDate since);

    MigrationRunResponse startMigration(MigrationStartRequest request, Long requestedBy);

    List<MigrationRunResponse> listMigrations(int limit);

    MigrationRunResponse getMigration(UUID id);

    MigrationRunResponse cancelMigration(UUID id);
}
