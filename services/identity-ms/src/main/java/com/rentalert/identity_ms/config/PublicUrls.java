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
    public static final String EMAIL_TAG = PROFILE_TAG + "/email";
    public static final String APP_SETTINGS_TAG = PREFIX + "/app-settings";
    public static final String INTERNAL_PROFILE_TAG = PREFIX + INTERNAL_PREFIX + "/profiles";
    public static final String INTERNAL_FILTER_TAG = PREFIX + INTERNAL_PREFIX + "/filters";
    public static final String INTERNAL_ADMIN_TAG = PREFIX + INTERNAL_PREFIX + "/admin";

    public static final RequestMatcher PUBLIC_URLS = new OrRequestMatcher(
        PathPatternRequestMatcher.withDefaults().matcher(PREFIX + "/temp/**"),
        PathPatternRequestMatcher.withDefaults().matcher(PREFIX + "/auth/login/**"),
        PathPatternRequestMatcher.withDefaults().matcher(PREFIX + "/auth/refresh-token")
    );
}