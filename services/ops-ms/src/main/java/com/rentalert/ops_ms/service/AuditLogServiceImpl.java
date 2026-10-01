package com.rentalert.ops_ms.service;

import com.rentalert.dto.audit.AuditLogResponse;
import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.ops_ms.domain.AuditLog;
import com.rentalert.ops_ms.mapper.AuditLogMapper;
import com.rentalert.ops_ms.repository.AuditLogRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional
    public void record(AdminActionEvent event) {
        if (auditLogRepository.existsByEventId(event.getIdempotencyKey())) {
            log.debug("Audit event {} already recorded, skipping", event.getIdempotencyKey());
            return;
        }
        auditLogRepository.save(auditLogMapper.toEntity(event));
    }

    @Override
    @Transactional
    public void recordUserAction(UserActionEvent event) {
        if (auditLogRepository.existsByEventId(event.getIdempotencyKey())) {
            log.debug("User event {} already recorded, skipping", event.getIdempotencyKey());
            return;
        }
        auditLogRepository.save(auditLogMapper.toEntity(event));
    }

    @Override
    public Page<AuditLogResponse> findAll(EventInitiatorType eventInitiator, Long userId, Pageable pageable) {
        return auditLogRepository.search(eventInitiator, userId, pageable)
            .map(auditLogMapper::toResponse);
    }

    @Override
    @Transactional
    public int cleanUp(int retentionDays) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = auditLogRepository.deleteAllByCreatedAtBefore(cutoff);
        log.info("Deleted {} audit log entries older than {}", deleted, cutoff);
        return deleted;
    }
}
