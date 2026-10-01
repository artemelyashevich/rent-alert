package com.rentalert.notification_ms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket (with SockJS fallback) for realtime user notifications, enabled by {@code ws.enabled}.
 * <p>
 * Clients connect to {@link PublicUrls#WEBSOCKET_ENDPOINT} and subscribe to {@code /user/queue/notifications}.
 * The in-memory broker keeps subscriptions per instance, so notifications reach only users connected to the instance
 * that dispatches them: run a single notification-ms instance while this broker is used.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@ConditionalOnProperty(value = "ws.enabled", havingValue = "true")
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(PublicUrls.WEBSOCKET_ENDPOINT)
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
