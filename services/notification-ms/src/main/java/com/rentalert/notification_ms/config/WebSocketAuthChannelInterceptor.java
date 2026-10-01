package com.rentalert.notification_ms.config;

import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.util.auth.JwtType;
import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import com.rentalert.redis.service.impl.SessionAuthenticationService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Authenticates the STOMP {@code CONNECT} frame by its {@code Authorization: Bearer <access token>} header.
 * <p>
 * The token must be a valid access token with an active, not blocked session in Redis. The connection's principal
 * name is the user id, which is what {@code convertAndSendToUser} addresses.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "ws.enabled", havingValue = "true")
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final CacheService cacheService;
    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = extractToken(accessor);
        if (token == null) {
            throw new MessagingException("Missing Bearer token in STOMP CONNECT");
        }

        UserSession session;
        try {
            jwtService.validateToken(token, JwtType.ACCESS);
            Long userId = jwtService.extractUserId(token);
            Optional<UserSession> found = cacheService.get(CacheType.USER_SESSION, token);
            session = found.filter(s -> Objects.equals(s.getUserId(), userId)).orElse(null);
        } catch (Exception e) {
            log.warn("WS CONNECT rejected: {}", e.getMessage());
            throw new MessagingException("WebSocket authentication failed");
        }
        if (session == null) {
            throw new MessagingException("WebSocket authentication failed");
        }
        if (SessionAuthenticationService.BLOCKED_ROLE.equals(session.getRole())) {
            throw new MessagingException("User is blocked");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(session.getUserId().toString(), null, List.of()));
        log.debug("WS CONNECT authenticated. userId={}", session.getUserId());
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
