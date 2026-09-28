package com.rentalert.identity_ms.security;

import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public final class PublicUrls {

    private static final String PREFIX = "/api/v1/";

    public static final RequestMatcher PUBLIC_URLS = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/actuator/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/error"),
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-resources/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html"),
            PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/scheduler-settings"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/scheduler-settings/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/webjars/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/ws/**"),
            PathPatternRequestMatcher.withDefaults().matcher(PREFIX + "auth/**")
    );

    private PublicUrls() {
        throw new UnsupportedOperationException();
    }
}