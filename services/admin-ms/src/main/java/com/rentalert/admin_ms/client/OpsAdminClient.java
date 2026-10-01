package com.rentalert.admin_ms.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.audit.AuditLogResponse;
import com.rentalert.dto.event.EventInitiatorType;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
}
