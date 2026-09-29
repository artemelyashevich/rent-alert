package com.rentalert.common.service.impl;

import com.rentalert.common.service.LockService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Распределённый лок на Redis: {@code SET key token NX PX lease}, снятие — Lua-скриптом только своим токеном,
 * чтобы не снять лок, перехваченный другим инстансом после истечения lease.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisLockServiceImpl implements LockService {

    private static final Duration LOCK_WAIT_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration DEFAULT_LEASE_TIME = Duration.ofMinutes(5);

    private static final long RETRY_INTERVAL_MS = 100;

    private static final String LOCK_KEY_PREFIX = "lock:";

    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public void withLock(String lockName, Runnable action) {
        withLock(lockName, () -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T withLock(@NonNull String lockName, @NonNull Supplier<T> action) {
        String key = resolveKey(lockName);
        String token = acquireLock(key, lockName);
        if (token == null) {
            throw new IllegalStateException("Unable to acquire lock: " + lockName);
        }
        try {
            return action.get();
        } finally {
            releaseLock(key, token, lockName);
        }
    }

    @Override
    public boolean tryWithLock(@NonNull String lockName, @NonNull Duration leaseTime, @NonNull Runnable action) {
        String key = resolveKey(lockName);
        String token = UUID.randomUUID().toString();

        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, token, leaseTime))) {
            log.debug("Lock is busy, skipping action: {}", lockName);
            return false;
        }

        try {
            action.run();
            return true;
        } finally {
            releaseLock(key, token, lockName);
        }
    }

    @Override
    public boolean isLocked(@NonNull String lockName) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(resolveKey(lockName)));
    }

    private String acquireLock(String key, String lockName) {
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + LOCK_WAIT_TIMEOUT.toMillis();
        while (true) {
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, token, DEFAULT_LEASE_TIME))) {
                log.debug("Lock acquired: {}", lockName);
                return token;
            }
            if (System.currentTimeMillis() >= deadline) {
                log.warn("Timeout while acquiring lock: {}", lockName);
                return null;
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrupted while acquiring lock: {}", lockName, e);
                return null;
            }
        }
    }

    private void releaseLock(String key, String token, String lockName) {
        try {
            Long released = redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
            if (Long.valueOf(1).equals(released)) {
                log.debug("Lock released: {}", lockName);
            } else {
                log.warn("Lock is no longer held by the current owner at release time: {}", lockName);
            }
        } catch (Exception e) {
            log.error("Failed to release lock: {}", lockName, e);
        }
    }

    private String resolveKey(String lockName) {
        return LOCK_KEY_PREFIX + lockName;
    }
}
