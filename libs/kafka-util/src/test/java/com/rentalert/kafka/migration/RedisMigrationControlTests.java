package com.rentalert.kafka.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.service.LockService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RedisMigrationControlTests {

    private final CacheService cacheService = mock(CacheService.class);
    private final LockService lockService = mock(LockService.class);
    private final RedisMigrationControl control = new RedisMigrationControl(cacheService, lockService);
    private final UUID migrationId = UUID.randomUUID();

    @Test
    void cancelledWhenFlagIsPresent() {
        when(cacheService.contains(CacheType.MIGRATION_CANCELLED, migrationId)).thenReturn(true);

        assertThat(control.isCancelled(migrationId)).isTrue();
    }

    @Test
    void notCancelledWhenFlagIsAbsent() {
        when(cacheService.contains(CacheType.MIGRATION_CANCELLED, migrationId)).thenReturn(false);

        assertThat(control.isCancelled(migrationId)).isFalse();
    }

    @Test
    void redisErrorIsTreatedAsNotCancelled() {
        when(cacheService.contains(any(), any())).thenThrow(new IllegalStateException("redis is down"));

        assertThat(control.isCancelled(migrationId)).isFalse();
    }

    @Test
    void lockIsDelegatedToLockService() {
        Runnable action = () -> { };
        Duration lease = Duration.ofMinutes(5);
        when(lockService.tryWithLock(anyString(), eq(lease), any())).thenReturn(true);

        assertThat(control.tryWithLock("migration:x", lease, action)).isTrue();

        verify(lockService).tryWithLock("migration:x", lease, action);
    }
}
