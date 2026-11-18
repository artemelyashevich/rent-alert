package com.rentalert.roommate_ms.service;

import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoommateBatchCache {

    private final CacheService cacheService;

    public <K, V> Map<K, V> getAll(CacheType cacheType, Collection<K> keys, Duration ttl,
                                   Function<Collection<K>, Map<K, V>> loader) {
        Set<K> requested = new LinkedHashSet<>(keys);
        Map<K, V> result = readCached(cacheType, requested);
        List<K> missing = requested.stream().filter(key -> !result.containsKey(key)).toList();
        if (missing.isEmpty()) {
            return result;
        }
        Map<K, V> loaded;
        try {
            loaded = loader.apply(missing);
        } catch (RuntimeException e) {
            log.warn("Batch load failed, serving cached entries only. cache={}, missing={}: {}",
                cacheType, missing.size(), e.getMessage());
            return result;
        }
        Map<K, V> fresh = new HashMap<>();
        loaded.forEach((key, value) -> {
            if (value != null) {
                fresh.put(key, value);
            }
        });
        writeCached(cacheType, fresh, ttl);
        result.putAll(fresh);
        return result;
    }

    public <K> void evictAll(CacheType cacheType, Collection<K> keys) {
        try {
            keys.forEach(key -> cacheService.evict(cacheType, key));
        } catch (RuntimeException e) {
            log.warn("Cache evict failed, entries expire by TTL. cache={}: {}", cacheType, e.getMessage());
        }
    }

    private <K, V> Map<K, V> readCached(CacheType cacheType, Collection<K> keys) {
        Map<K, V> cached = new HashMap<>();
        try {
            for (K key : keys) {
                cacheService.<V>get(cacheType, key).ifPresent(value -> cached.put(key, value));
            }
        } catch (RuntimeException e) {
            log.warn("Cache read failed, loading from source. cache={}: {}", cacheType, e.getMessage());
            return new HashMap<>();
        }
        return cached;
    }

    private <K, V> void writeCached(CacheType cacheType, Map<K, V> values, Duration ttl) {
        try {
            values.forEach((key, value) -> cacheService.put(cacheType, key, value, ttl));
        } catch (RuntimeException e) {
            log.warn("Cache write failed, entries are not cached. cache={}: {}", cacheType, e.getMessage());
        }
    }
}
