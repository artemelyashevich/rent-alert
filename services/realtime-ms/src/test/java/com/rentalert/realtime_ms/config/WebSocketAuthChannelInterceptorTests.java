package com.rentalert.realtime_ms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AuthException;
import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.util.auth.JwtType;
import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

class WebSocketAuthChannelInterceptorTests {

    private final CacheService cacheService = mock(CacheService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(cacheService, jwtService);

    private static Message<byte[]> frame(StompCommand command, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private void session(String role) {
        when(jwtService.extractUserId("token")).thenReturn(5L);
        when(cacheService.get(CacheType.USER_SESSION, "token"))
            .thenReturn(Optional.of(UserSession.builder().userId(5L).role(role).build()));
    }

    @Test
    void connectWithActiveSessionIsBoundToUserId() {
        session("USER");

        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, "Bearer token"), channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor.getUser().getName()).isEqualTo("5");
    }

    @Test
    void connectWithoutTokenIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null), channel))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void connectWithoutSessionIsRejected() {
        when(jwtService.extractUserId("token")).thenReturn(5L);
        when(cacheService.get(CacheType.USER_SESSION, "token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer token"), channel))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void connectWithInvalidTokenIsRejected() {
        doThrow(new AuthException("expired")).when(jwtService).validateToken("token", JwtType.ACCESS);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer token"), channel))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void blockedUserIsRejected() {
        session("BLOCKED");

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer token"), channel))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void otherFramesPassThrough() {
        Message<byte[]> subscribe = frame(StompCommand.SUBSCRIBE, null);

        assertThat(interceptor.preSend(subscribe, channel)).isSameAs(subscribe);
    }
}
