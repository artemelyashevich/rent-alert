package com.rentalert.ops_ms.controller.internal;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.audit.AuditLogResponse;
import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.ops_ms.configuration.PublicUrls;
import com.rentalert.ops_ms.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG + "/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogInternalController {

    private final AuditLogService auditLogService;

    @GetMapping
    public PageResponse<AuditLogResponse> findAll(
        @RequestParam(required = false) EventInitiatorType eventInitiator,
        @RequestParam(required = false) Long userId,
        Pageable pageable
    ) {
        return PageUtil.toResponse(auditLogService.findAll(eventInitiator, userId, pageable));
    }

    @DeleteMapping
    public int cleanUp(@RequestParam(defaultValue = "4") int retentionDays) {
        return auditLogService.cleanUp(retentionDays);
    }
}
