package com.rentalert.identity_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

class ProfileCacheEvictorTests {

    private final CacheService cacheService = mock(CacheService.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final ProfileCacheEvictor evictor = new ProfileCacheEvictor(cacheService, scheduler);

    @Test
    void evictsAllUserCachesNow() {
        evictor.evict(42L);

        verify(cacheService).evict(CacheType.USER_PROFILE, 42L);
        verify(cacheService).evict(CacheType.USER_PROFILE_INTERNAL, 42L);
        verify(cacheService).evict(CacheType.USER_ACTIVE_FILTER, 42L);
    }

    @Test
    void repeatsEvictionAfterDelay() {
        Instant before = Instant.now();

        evictor.evict(42L);

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> at = ArgumentCaptor.forClass(Instant.class);
        verify(scheduler).schedule(task.capture(), at.capture());
        assertThat(Duration.between(before, at.getValue())).isGreaterThanOrEqualTo(ProfileCacheEvictor.REPEAT_DELAY);

        task.getValue().run();
        verify(cacheService, times(2)).evict(CacheType.USER_PROFILE_INTERNAL, 42L);
        verify(cacheService, times(2)).evict(CacheType.USER_ACTIVE_FILTER, 42L);
    }
}
