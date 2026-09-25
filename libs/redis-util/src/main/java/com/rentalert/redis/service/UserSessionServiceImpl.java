package com.rentalert.redis.service;

import com.rentalert.common.auth.AuthUtil;
import com.rentalert.common.auth.JwtService;
import com.rentalert.common.exception.AuthException;
import com.rentalert.dto.user.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    // Redis key prefixes; every service must use the same ones to see each other's sessions
    public static final String USER_SESSION = "USER_SESSION";
    public static final String USER_ACTIVE_TOKEN = "USER_ACTIVE_TOKEN";

    private final CacheService cacheService;
    private final HttpServletRequest httpServletRequest;
    private final JwtService jwtService;

    private static final long MINIMUM_TTL_SECONDS = 60;

    @Override
    public UserSession getCurrentUser() {
        String token = AuthUtil.getToken(httpServletRequest);
        Optional<UserSession> userSession = cacheService.get(USER_SESSION, token, UserSession.class);
        if (userSession.isPresent()) {
            return userSession.get();
        }
        throw new AuthException("There are no active sessions...");
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
            .token(token)
            .build();
        log.info("Creating session for user id: {} with TTL: {}s (token expires at: {})", userId, ttl.getSeconds(), jwtService.extractExpiration(token));

        cacheService.put(USER_SESSION, token, userSession, ttl);
        cacheService.put(USER_ACTIVE_TOKEN, userId.toString(), token, ttl);

        log.debug("Session created for user id: {} with ttl: {}s", userId, ttl.getSeconds());
        return userSession;
    }

    @Override
    public void updateSession(UserSession session) {
        String token = AuthUtil.getToken(httpServletRequest);
        createSession(token, session);
    }

    @Override
    public void invalidateSession(Long userId) {
        try {
            cacheService.get(USER_ACTIVE_TOKEN, userId.toString(), String.class)
                .ifPresent(previousToken -> {
                    try {
                        cacheService.evict(USER_SESSION, previousToken);
                        cacheService.evict(USER_ACTIVE_TOKEN, userId.toString());
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