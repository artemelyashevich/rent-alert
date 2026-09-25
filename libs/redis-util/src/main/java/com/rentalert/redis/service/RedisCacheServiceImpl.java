package com.rentalert.redis.service;

import com.rentalert.common.text.JsonHelper;

import tools.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements CacheService {

    private static final int BATCH_SIZE = 250;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    @Override
    public <T> Optional<T> get(String cacheName, Object key, TypeReference<T> type) {
        String redisKey = buildKey(cacheName, key);
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            log.debug("Cache MISS. cache={}, key={}", cacheName, key);
            return Optional.empty();
        }
        T value = JsonHelper.fromJson(json, type);
        log.debug("Cache HIT. cache={}, key={}", cacheName, key);
        return Optional.ofNullable(value);
    }

    @Override
    public <T> T get(String cacheName, Object key, Class<T> type, Supplier<T> loader) {
        Optional<T> cached = get(cacheName, key, type);

        if (cached.isPresent()) {
            log.debug("Cache HIT. cache={}, key={}", cacheName, key);
            return cached.get();
        }

        T loaded = loader.get();

        if (loaded == null) {
            throw new IllegalStateException("Cache loader returned null");
        }

        put(cacheName, key, loaded);

        log.debug("Cache MISS. cache={}, key={}", cacheName, key);

        return loaded;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String cacheName, Object key, Class<T> type) {
        String redisKey = buildKey(cacheName, key);

        String json = redisTemplate.opsForValue().get(redisKey);

        if (json == null) {
            return Optional.empty();
        }

        T value = (T) JsonHelper.fromJson(json, type);

        return Optional.ofNullable(value);
    }

    @Override
    public void put(String cacheName, Object key, Object value) {
        put(cacheName, key, value, DEFAULT_TTL);
    }

    @Override
    public void put(String cacheName, Object key, Object value, Duration ttl) {
        requireNonNullValue(cacheName, key, value);

        String redisKey = buildKey(cacheName, key);

        String json = JsonHelper.toJson(value);

        redisTemplate.opsForValue().set(redisKey, json, ttl);

        log.debug("Cache PUT. cache={}, key={}, ttl={}", cacheName, key, ttl);
    }

    @Override
    public boolean putIfAbsent(String cacheName, Object key, Object value) {
        return putIfAbsent(cacheName, key, value, DEFAULT_TTL);
    }

    @Override
    public void evict(String cacheName, Object key) {
        redisTemplate.delete(buildKey(cacheName, key));
        log.debug("Cache EVICT. cache={}, key={}", cacheName, key);
    }

    @Override
    public void evict(String cacheName) {
        redisTemplate.delete(cacheName);
        log.debug("Cache EVICT-ALL. cache={}", cacheName);
    }

    @Override
    public void clear(String cacheName) {
        redisTemplate.delete(cacheName);

        ScanOptions options = ScanOptions.scanOptions()
                .match(cacheName + ":*")
                .count(1000)
                .build();

        try (RedisConnection connection = Objects.requireNonNull(
                redisTemplate.getConnectionFactory()
        ).getConnection()) {

            List<byte[]> keysToDelete = new ArrayList<>();

            try (Cursor<byte[]> cursor = connection.scan(options)) {
                cursor.forEachRemaining(keysToDelete::add);
            }

            if (!keysToDelete.isEmpty()) {
                connection.keyCommands().del(keysToDelete.toArray(byte[][]::new));
            }

        } catch (Exception e) {
            log.error("Cache CLEAR failed. cache={}", cacheName, e);
            throw new RuntimeException("Failed to clear cache: " + cacheName, e);
        }

        log.debug("Cache CLEAR. cache={}", cacheName);
    }

    @Override
    public Map<String, Object> stats() {
        return Map.of(
                "redis", "stats unsupported"
        );
    }

    @Override
    public boolean putIfAbsent(String cacheName, Object key, Object value, Duration ttl) {
        requireNonNullValue(cacheName, key, value);

        String redisKey = buildKey(cacheName, key);

        String json = JsonHelper.toJson(value);

        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, json, ttl);

        if (Boolean.TRUE.equals(success)) {
            log.debug("Cache ACQUIRED. cache={}, key={}", cacheName, key);
        }

        return Boolean.TRUE.equals(success);
    }

    @Override
    public boolean contains(String cacheName, Object key) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(
                        buildKey(cacheName, key)
                )
        );
    }

    @Override
    public long increment(String cacheName, Object key) {
        String redisKey = buildKey(cacheName, key);
        Long value = redisTemplate.opsForValue().increment(redisKey);
        return value == null ? 0L : value;
    }

    @Override
    public void addToSet(String cacheName, String value, Duration ttl) {
        Long added = redisTemplate.opsForSet().add(cacheName, value);

        if (Long.valueOf(1).equals(added)) {
            redisTemplate.expire(cacheName, ttl);
        }

        log.debug("Cache SET ADD. cache={}, value={}", cacheName, value);
    }

    @Override
    public void addToSetBatch(String cacheName, List<String> values, Duration ttl) {
        if (values == null || values.isEmpty()) {
            return;
        }

        for (int i = 0; i < values.size(); i += BATCH_SIZE) {
            List<String> batch = values.subList(i, Math.min(i + BATCH_SIZE, values.size()));
            redisTemplate.opsForSet().add(cacheName, batch.toArray(new String[0]));
        }
        redisTemplate.expire(cacheName, ttl);
        log.debug("Cache SET ADD BATCH. cache={}, count={}", cacheName, values.size());
    }

    @Override
    public boolean isMember(String cacheName, String value) {
        Boolean result = redisTemplate.opsForSet().isMember(cacheName, value);
        return Boolean.TRUE.equals(result);
    }

    private String buildKey(String cacheName, Object key) {
        return cacheName + ":" + key;
    }

    private void requireNonNullValue(String cacheName, Object key, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    String.format(
                            "Cache value must not be null. cache=%s, key=%s",
                            cacheName,
                            key
                    )
            );
        }
    }
}