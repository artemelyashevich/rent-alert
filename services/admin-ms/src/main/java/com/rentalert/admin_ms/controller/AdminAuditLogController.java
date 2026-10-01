package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.client.OpsAdminClient;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.audit.AuditLogResponse;
import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.event.admin.AdminEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin audit log", description = "Журнал аудита")
@RestController
@RequestMapping(PublicUrls.AUDIT_LOG_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final OpsAdminClient opsAdminClient;
    private final AdminAuditPublisher auditPublisher;

    @Operation(summary = "Получить журнал аудита")
    @GetMapping
    public PageResponse<AuditLogResponse> findAll(
        @RequestParam(required = false) EventInitiatorType eventInitiator,
        @RequestParam(required = false) Long userId,
        @ParameterObject Pageable pageable
    ) {
        return opsAdminClient.findAuditLogs(eventInitiator, userId,
            pageable.getPageNumber(), pageable.getPageSize(), PageUtil.toSortParams(pageable));
    }

    @Operation(summary = "Удалить записи журнала аудита старше retentionDays дней")
    @DeleteMapping
    public int cleanUp(@RequestParam(defaultValue = "4") int retentionDays) {
        int deleted = opsAdminClient.cleanUpAuditLogs(retentionDays);
        auditPublisher.publish(AdminEvent.AUDIT_LOG_CLEAN_UP);
        return deleted;
    }
}
