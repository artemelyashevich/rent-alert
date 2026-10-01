package com.rentalert.internal.client.service.impl;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.audit.AuditLogResponse;
import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.job.JobSettingResponse;
import com.rentalert.dto.statistics.SystemActivityOverview;
import com.rentalert.dto.statistics.UserActionSummary;
import com.rentalert.dto.statistics.UserActivityLog;
import com.rentalert.dto.statistics.UserStatisticsResponse;
import com.rentalert.internal.client.AbstractResilientClient;
import com.rentalert.internal.client.OpsMsClient;
import com.rentalert.internal.client.service.OpsMsService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public class OpsMsServiceImpl extends AbstractResilientClient implements OpsMsService {

    private final OpsMsClient opsMsClient;

    public OpsMsServiceImpl(OpsMsClient opsMsClient, RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        super(OpsMsClient.SERVICE_NAME, retryRegistry, circuitBreakerRegistry);
        this.opsMsClient = opsMsClient;
    }

    @Override
    public PageResponse<AuditLogResponse> findAuditLogs(EventInitiatorType eventInitiator, Long userId, Pageable pageable) {
        return execute(() -> opsMsClient.findAuditLogs(eventInitiator, userId, pageable.getPageNumber(), pageable.getPageSize(),
            PageUtil.toSortParams(pageable)));
    }

    @Override
    public int cleanUpAuditLogs(int retentionDays) {
        return executeOnce(() -> opsMsClient.cleanUpAuditLogs(retentionDays));
    }

    @Override
    public PageResponse<JobSettingResponse> findJobSettings(Pageable pageable) {
        return execute(() -> opsMsClient.findJobSettings(pageable.getPageNumber(), pageable.getPageSize(),
            PageUtil.toSortParams(pageable)));
    }

    @Override
    public UUID startCleanup() {
        return executeOnce(opsMsClient::startCleanup);
    }

    @Override
    public UserStatisticsResponse getUserStatistics(Long userId, Long filterId) {
        return execute(() -> opsMsClient.getUserStatistics(userId, filterId));
    }

    @Override
    public List<UserActionSummary> getUserActionSummary(Long userId, LocalDate since, int limit) {
        return execute(() -> opsMsClient.getUserActionSummary(userId, since.toString(), limit));
    }

    @Override
    public List<UserActivityLog> getUserActivityLog(Long userId, LocalDate since, LocalDate until) {
        return execute(() -> opsMsClient.getUserActivityLog(userId, since.toString(), until == null ? null : until.toString()));
    }

    @Override
    public SystemActivityOverview getSystemActivity(LocalDate since) {
        return execute(() -> opsMsClient.getSystemActivity(since.toString()));
    }
}
