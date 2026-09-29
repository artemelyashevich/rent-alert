package com.rentalert.identity_ms.service.impl;

import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ProfileCacheEvictor {

    static final Duration REPEAT_DELAY = Duration.ofSeconds(10);

    private final CacheService cacheService;
    private final TaskScheduler cacheEvictionScheduler;

    public void evict(Long userId) {
        evictNow(userId);
        cacheEvictionScheduler.schedule(() -> evictNow(userId), Instant.now().plus(REPEAT_DELAY));
    }

    private void evictNow(Long userId) {
        cacheService.evict(CacheType.USER_PROFILE, userId);
        cacheService.evict(CacheType.USER_PROFILE_INTERNAL, userId);
        cacheService.evict(CacheType.USER_ACTIVE_FILTER, userId);
    }
}
