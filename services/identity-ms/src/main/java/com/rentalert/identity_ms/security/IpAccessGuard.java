package com.rentalert.identity_ms.security;

import com.rentalert.identity_ms.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpAccessGuard {

    private static final String NO_TOKEN_COUNT_KEY_PREFIX = "security:ip:no-token-count:";
    private static final String BLOCKED_KEY_PREFIX = "security:ip:blocked:";
    private static final String RATE_LIMIT_COUNT_KEY_PREFIX = "security:ip:rate-count:";

    private final StringRedisTemplate redisTemplate;
    private final ApplicationProperties applicationProperties;

    public boolean isBlocked(String ip) {
        if (!config().isEnabled()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLOCKED_KEY_PREFIX + ip));
    }

    public void registerRequestWithoutToken(String ip) {
        ApplicationProperties.IpRateLimit config = config();
        if (!config.isEnabled()) {
            return;
        }

        String countKey = NO_TOKEN_COUNT_KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(countKey, Duration.ofSeconds(config.getWindowSeconds()));
        }

        if (count == null || count <= config.getMaxRequestsWithoutToken()) {
            return;
        }

        String blockKey = BLOCKED_KEY_PREFIX + ip;
        Boolean firstBlock = redisTemplate.opsForValue()
                .setIfAbsent(blockKey, Instant.now().toString(), Duration.ofSeconds(config.getBlockDurationSeconds()));

        if (Boolean.TRUE.equals(firstBlock)) {
            log.warn(
                    "[IP GUARD] Blocked IP {} for {}s after {} requests without a Bearer token within {}s",
                    ip, config.getBlockDurationSeconds(), count, config.getWindowSeconds()
            );
        }
    }

    public boolean isRateLimited(String ip) {
        ApplicationProperties.RequestRateLimit config = rateLimitConfig();
        if (!config.isEnabled()) {
            return false;
        }

        String countKey = RATE_LIMIT_COUNT_KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(countKey, Duration.ofSeconds(config.getWindowSeconds()));
        }

        if (count == null) {
            return false;
        }

        if (count == config.getRequestsPerWindow() + 1L) {
            log.warn(
                    "[RATE LIMIT] IP {} exceeded {} requests within {}s - throttling",
                    ip, config.getRequestsPerWindow(), config.getWindowSeconds()
            );
        }

        return count > config.getRequestsPerWindow();
    }

    private ApplicationProperties.IpRateLimit config() {
        return applicationProperties.getSecurity().getIpRateLimit();
    }

    private ApplicationProperties.RequestRateLimit rateLimitConfig() {
        return applicationProperties.getSecurity().getRateLimit();
    }
}
