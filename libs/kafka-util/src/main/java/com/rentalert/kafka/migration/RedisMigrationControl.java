package com.rentalert.kafka.migration;

import com.rentalert.common.service.LockService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class RedisMigrationControl implements MigrationControl {

    private final CacheService cacheService;
    private final LockService lockService;

    @Override
    public boolean isCancelled(UUID migrationId) {
        try {
            return cacheService.contains(CacheType.MIGRATION_CANCELLED, migrationId);
        } catch (RuntimeException e) {
            log.warn("[MIGRATION] {}: cancellation flag is unavailable, continuing: {}", migrationId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean tryWithLock(String lockName, Duration leaseTime, Runnable action) {
        return lockService.tryWithLock(lockName, leaseTime, action);
    }
}
