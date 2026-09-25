package com.rentalert.redis.service;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface CacheService {

    <T> Optional<T> get( String cacheName, Object key, TypeReference<T> type );

    <T> T get(String cacheName, Object key, Class<T> type, Supplier<T> loader);

    <T> Optional<T> get(String cacheName, Object key, Class<T> type);

    void put(String cacheName, Object key, Object value);

    void put(String cacheName, Object key, Object value, Duration ttl);

    void evict(String cacheName, Object key);

    void evict(String cacheName);

    void clear(String cacheName);

    Map<String, Object> stats();

    boolean putIfAbsent(String cacheName, Object key, Object value, Duration ttl);

    boolean putIfAbsent(String cacheName, Object key, Object value);

    boolean contains(String cacheName, Object key);

    long increment(String cacheName, Object key);

    void addToSet(String cacheName, String value, Duration ttl);

    void addToSetBatch(String cacheName, List<String> values, Duration ttl);

    boolean isMember(String cacheName, String value);
}