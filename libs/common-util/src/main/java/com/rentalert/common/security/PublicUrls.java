package com.rentalert.common.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PublicUrls {

    public static final RequestMatcher PUBLIC_URLS = new OrRequestMatcher(
        PathPatternRequestMatcher.withDefaults().matcher("/actuator/**"),
        PathPatternRequestMatcher.withDefaults().matcher("/swagger-resources/**"),
        PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
        PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html"),
        PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
        PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs")
    );

    private PublicUrls() {
        throw new UnsupportedOperationException();
    }
}