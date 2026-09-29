package com.rentalert.identity_ms.config;

import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class PublicUrls {

    public static final String PREFIX = "/identity";
    public static final String INTERNAL_PREFIX = "/internal";
    public static final String AUTH_TAG = PREFIX + "/auth";
    public static final String FEEDBACK_TAG = PREFIX + "/feedbacks";
    public static final String FILTER_TAG = PREFIX + "/filters";
    public static final String PROFILE_TAG = PREFIX + "/profiles";
    public static final String INTERNAL_PROFILE_TAG = PREFIX + INTERNAL_PREFIX + "/profiles";

    public static final RequestMatcher PUBLIC_URLS = new OrRequestMatcher(
        PathPatternRequestMatcher.withDefaults().matcher(PREFIX + "/temp/**"),
        PathPatternRequestMatcher.withDefaults().matcher(PREFIX + "/auth/login/**"),
        PathPatternRequestMatcher.withDefaults().matcher(PREFIX + "/auth/refresh-token")
    );
}