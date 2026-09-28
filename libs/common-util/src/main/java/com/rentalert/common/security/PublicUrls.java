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

    @Value("${spring.application.name}")
    private String prefix;

    private RequestMatcher publicUrls;

    @PostConstruct
    public void init() {
        publicUrls = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/" + prefix + "/v3/api-docs/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/" + prefix + "/swagger-ui/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/" + prefix + "/swagger-ui.html")
        );
    }
}