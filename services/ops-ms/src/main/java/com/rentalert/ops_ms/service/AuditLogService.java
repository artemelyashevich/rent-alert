package com.rentalert.ops_ms.service;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.ops_ms.domain.AuditLog;
import com.rentalert.ops_ms.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    AuditLog save(AuditLog auditLog);

    /**
     * Stores the admin action once: a redelivered event (same idempotency key) is ignored.
     */
    void record(AdminActionEvent event);

    /**
     * Stores the user action once: a redelivered event (same idempotency key) is ignored.
     */
    void recordUserAction(UserActionEvent event);

    Page<AuditLogResponse> findAll(EventInitiatorType eventInitiator, Long userId, Pageable pageable);

    int cleanUp(int retentionDays);
}
