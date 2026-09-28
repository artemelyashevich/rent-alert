package com.rentalert.redis.service;

import com.rentalert.redis.dto.CacheType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface CacheService {

    <T> Optional<T> get(CacheType cacheType, Object key);

    <T> T get(CacheType cacheType, Object key, Supplier<T> loader);

    void put(CacheType cacheType, Object key, Object value);

    void put(CacheType cacheType, Object key, Object value, Duration ttl);

    void evict(CacheType cacheType, Object key);

    void evict(CacheType cacheType);

    void clear(CacheType cacheType);

    Map<String, Object> stats();

    boolean putIfAbsent(CacheType cacheType, Object key, Object value, Duration ttl);

    boolean putIfAbsent(CacheType cacheType, Object key, Object value);

    boolean contains(CacheType cacheType, Object key);

    long increment(CacheType cacheType, Object key);

    void addToSet(CacheType cacheType, String value, Duration ttl);

    void addToSetBatch(CacheType cacheType, List<String> values, Duration ttl);

    boolean isMember(CacheType cacheType, String value);
}
