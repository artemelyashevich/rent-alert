package com.rentalert.redis.service.impl;

import com.rentalert.common.exception.AuthException;
import com.rentalert.common.util.auth.HeaderUtil;
import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.util.auth.JwtType;
import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import com.rentalert.redis.service.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private static final long MINIMUM_TTL_SECONDS = 60;
    private final CacheService cacheService;
    private final HttpServletRequest httpServletRequest;
    private final JwtService jwtService;

    @Override
    public UserSession getCurrentUser() {
        String token = HeaderUtil.getToken(httpServletRequest);
        Optional<UserSession> userSession = cacheService.get(CacheType.USER_SESSION, token);
        if (userSession.isPresent()) {
            return userSession.get();
        }
        throw new AuthException("There are no active sessions...");
    }

    @Override
    public UserSession createSession(UserSession session) {
        return createSession(jwtService.generateToken(session, JwtType.ACCESS), session);
    }

    @Override
    public UserSession createSession(String token, UserSession session) {
        Long userId = jwtService.extractUserId(token);
        Duration ttl = resolveTtl(token);
        invalidateSession(userId);
        UserSession userSession = UserSession.builder()
            .userId(session.getUserId())
            .username(session.getUsername())
            .email(session.getEmail())
            .role(session.getRole())
            .token(token)
            .build();
        log.info("Creating session for user id: {} with TTL: {}s (token expires at: {})", userId, ttl.getSeconds(), jwtService.extractExpiration(token));

        cacheService.put(CacheType.USER_SESSION, token, userSession, ttl);
        cacheService.put(CacheType.USER_ACTIVE_TOKEN, userId.toString(), token, ttl);

        log.debug("Session created for user id: {} with ttl: {}s", userId, ttl.getSeconds());
        return userSession;
    }

    @Override
    public void updateSession(UserSession session) {
        String token = HeaderUtil.getToken(httpServletRequest);
        createSession(token, session);
    }

    @Override
    public void invalidateSession(Long userId) {
        try {
            cacheService.get(CacheType.USER_ACTIVE_TOKEN, userId.toString())
                .ifPresent(previousToken -> {
                    try {
                        cacheService.evict(CacheType.USER_SESSION, previousToken);
                        cacheService.evict(CacheType.USER_ACTIVE_TOKEN, userId.toString());
                        log.debug("Session invalidated for user id: {}", userId);
                    } catch (Exception e) {
                        log.warn("Failed to evict session for user id: {}", userId, e);
                    }
                });
        } catch (Exception e) {
            log.warn("Failed to retrieve session token for user id: {}", userId, e);
        }
    }

    private Duration resolveTtl(String token) {
        Instant expiration = jwtService.extractExpiration(token).toInstant();
        Instant now = Instant.now();
        Duration ttl = Duration.between(now, expiration);

        if (ttl.isNegative()) {
            log.warn("Token has already expired! Expiration: {}, now: {}", expiration, now);
            throw new AuthException("Token has expired");
        }

        if (ttl.getSeconds() < MINIMUM_TTL_SECONDS) {
            log.warn("Token lifetime is too short: {}s. Setting minimum TTL: {}s",
                ttl.getSeconds(), MINIMUM_TTL_SECONDS);
            return Duration.ofSeconds(MINIMUM_TTL_SECONDS);
        }

        return ttl;
    }
}