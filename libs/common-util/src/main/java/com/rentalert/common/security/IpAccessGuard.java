package com.rentalert.common.security;

import com.rentalert.common.config.IpRateLimitProperties;
import com.rentalert.common.config.RequestRateLimitProperties;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@RequiredArgsConstructor
public class IpAccessGuard {

    private static final String NO_TOKEN_COUNT_KEY_PREFIX = "security:ip:no-token-count:";
    private static final String BLOCKED_KEY_PREFIX = "security:ip:blocked:";
    private static final String RATE_LIMIT_COUNT_KEY_PREFIX = "security:ip:rate-count:";

    private final StringRedisTemplate redisTemplate;
    private final IpRateLimitProperties ipRateLimitProperties;
    private final RequestRateLimitProperties requestRateLimitProperties;

    public boolean isBlocked(String ip) {
        if (!config().isEnabled()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLOCKED_KEY_PREFIX + ip));
    }

    public void registerRequestWithoutToken(String ip) {
        IpRateLimitProperties config = config();
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
            log.warn("[IP GUARD] Blocked IP {} for {}s after {} requests without a Bearer token within {}s", ip, config.getBlockDurationSeconds(), count, config.getWindowSeconds());
        }
    }

    public boolean isRateLimited(String ip) {
        RequestRateLimitProperties config = rateLimitConfig();
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
            log.warn("[RATE LIMIT] IP {} exceeded {} requests within {}s - throttling", ip, config.getRequestsPerWindow(), config.getWindowSeconds());
        }
        return count > config.getRequestsPerWindow();
    }

    private IpRateLimitProperties config() {
        return ipRateLimitProperties;
    }

    private RequestRateLimitProperties rateLimitConfig() {
        return requestRateLimitProperties;
    }
}
