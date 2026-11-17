package com.rentalert.realtime_ms.config;

import com.rentalert.common.security.AbstractSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfiguration extends AbstractSecurityConfiguration {

    /**
     * The WebSocket handshake carries no token (browsers can't set headers on it); the STOMP CONNECT frame is
     * authenticated by {@link WebSocketAuthChannelInterceptor} instead.
     */
    @Override
    protected RequestMatcher publicUrls() {
        return PathPatternRequestMatcher.withDefaults().matcher(PublicUrls.WEBSOCKET_ENDPOINT + "/**");
    }
}
