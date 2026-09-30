package com.rentalert.redis.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.util.JsonHelper;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements CacheService {

    private static final int BATCH_SIZE = 250;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(CacheType cacheType, Object key) {
        String redisKey = buildKey(cacheType, key);
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            log.debug("Cache MISS. cache={}, key={}", cacheType, key);
            return Optional.empty();
        }
        try {
            T value = (T) JsonHelper.fromJson(json, cacheType.getType());
            log.debug("Cache HIT. cache={}, key={}", cacheType, key);
            return Optional.ofNullable(value);
        } catch (AppException e) {
            // An entry written by an older class version must not break the request: treat it as a miss
            log.warn("Cache entry is unreadable, evicting. cache={}, key={}", cacheType, key);
            redisTemplate.delete(redisKey);
            return Optional.empty();
        }
    }

    @Override
    public <T> T get(CacheType cacheType, Object key, Supplier<T> loader) {
        Optional<T> cached = get(cacheType, key);
        if (cached.isPresent()) {
            return cached.get();
        }

        T loaded = loader.get();
        if (loaded == null) {
            throw new IllegalStateException("Cache loader returned null");
        }
        put(cacheType, key, loaded);
        return loaded;
    }

    @Override
    public void put(CacheType cacheType, Object key, Object value) {
        put(cacheType, key, value, DEFAULT_TTL);
    }

    @Override
    public void put(CacheType cacheType, Object key, Object value, Duration ttl) {
        requireNonNullValue(cacheType, key, value);

        String redisKey = buildKey(cacheType, key);

        String json = JsonHelper.toJson(value);

        redisTemplate.opsForValue().set(redisKey, json, ttl);

        log.debug("Cache PUT. cache={}, key={}, ttl={}", cacheType, key, ttl);
    }

    @Override
    public boolean putIfAbsent(CacheType cacheType, Object key, Object value) {
        return putIfAbsent(cacheType, key, value, DEFAULT_TTL);
    }

    @Override
    public void evict(CacheType cacheType, Object key) {
        redisTemplate.delete(buildKey(cacheType, key));
        log.debug("Cache EVICT. cache={}, key={}", cacheType, key);
    }

    @Override
    public void evict(CacheType cacheType) {
        redisTemplate.delete(cacheType.name());
        log.debug("Cache EVICT-ALL. cache={}", cacheType);
    }

    @Override
    public void clear(CacheType cacheType) {
        redisTemplate.delete(cacheType.name());

        ScanOptions options = ScanOptions.scanOptions()
            .match(cacheType.name() + ":*")
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
            log.error("Cache CLEAR failed. cache={}", cacheType, e);
            throw new RuntimeException("Failed to clear cache: " + cacheType, e);
        }

        log.debug("Cache CLEAR. cache={}", cacheType);
    }

    @Override
    public Map<String, Object> stats() {
        return Map.of(
            "redis", "stats unsupported"
        );
    }

    @Override
    public boolean putIfAbsent(CacheType cacheType, Object key, Object value, Duration ttl) {
        requireNonNullValue(cacheType, key, value);

        String redisKey = buildKey(cacheType, key);

        String json = JsonHelper.toJson(value);

        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, json, ttl);

        if (Boolean.TRUE.equals(success)) {
            log.debug("Cache ACQUIRED. cache={}, key={}", cacheType, key);
        }

        return Boolean.TRUE.equals(success);
    }

    @Override
    public boolean contains(CacheType cacheType, Object key) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(
                buildKey(cacheType, key)
            )
        );
    }

    @Override
    public long increment(CacheType cacheType, Object key) {
        String redisKey = buildKey(cacheType, key);
        Long value = redisTemplate.opsForValue().increment(redisKey);
        return value == null ? 0L : value;
    }

    @Override
    public void addToSet(CacheType cacheType, String value, Duration ttl) {
        Long added = redisTemplate.opsForSet().add(cacheType.name(), value);

        if (Long.valueOf(1).equals(added)) {
            redisTemplate.expire(cacheType.name(), ttl);
        }

        log.debug("Cache SET ADD. cache={}, value={}", cacheType, value);
    }

    @Override
    public void addToSetBatch(CacheType cacheType, List<String> values, Duration ttl) {
        if (values == null || values.isEmpty()) {
            return;
        }

        for (int i = 0; i < values.size(); i += BATCH_SIZE) {
            List<String> batch = values.subList(i, Math.min(i + BATCH_SIZE, values.size()));
            redisTemplate.opsForSet().add(cacheType.name(), batch.toArray(new String[0]));
        }
        redisTemplate.expire(cacheType.name(), ttl);
        log.debug("Cache SET ADD BATCH. cache={}, count={}", cacheType, values.size());
    }

    @Override
    public boolean isMember(CacheType cacheType, String value) {
        Boolean result = redisTemplate.opsForSet().isMember(cacheType.name(), value);
        return Boolean.TRUE.equals(result);
    }

    private String buildKey(CacheType cacheType, Object key) {
        return cacheType.name() + ":" + key;
    }

    private void requireNonNullValue(CacheType cacheType, Object key, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                String.format(
                    "Cache value must not be null. cache=%s, key=%s",
                    cacheType,
                    key
                )
            );
        }
    }
}