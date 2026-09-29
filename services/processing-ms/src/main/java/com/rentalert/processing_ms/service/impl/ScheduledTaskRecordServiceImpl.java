package com.rentalert.processing_ms.service.impl;

import com.rentalert.processing_ms.domain.ScheduledTaskStatus;
import com.rentalert.processing_ms.repository.ScheduledTaskRecordRepository;
import com.rentalert.processing_ms.service.ScheduledTaskRecordService;
import com.rentalert.dto.listing.ScheduleProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskRecordServiceImpl implements ScheduledTaskRecordService {

    private final ScheduledTaskRecordRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPending(ScheduleProvider provider, String taskKey) {
        repository.upsert(provider.name(), taskKey, ScheduledTaskStatus.PENDING.name(), null, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(ScheduleProvider provider, String taskKey) {
        repository.upsert(provider.name(), taskKey, ScheduledTaskStatus.SUCCESS.name(), null, Instant.now());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markError(ScheduleProvider provider, String taskKey, Exception e) {
        repository.upsert(provider.name(), taskKey, ScheduledTaskStatus.ERROR.name(), buildErrorMessage(e), Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSuccessKeysForWarmUp(ScheduleProvider provider, Duration ttl) {
        Instant since = Instant.now().minus(ttl);
        return repository.findSuccessKeysByProviderSince(provider, since);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int deleteOlderThan(Instant cutoff) {
        int deleted = repository.deleteByCreatedAtBefore(cutoff);
        log.info("Deleted {} scheduled-task-record entries older than {}", deleted, cutoff);
        return deleted;
    }

    private String buildErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null && message.length() > 2000) {
            return message.substring(0, 2000);
        }
        return message;
    }
}
