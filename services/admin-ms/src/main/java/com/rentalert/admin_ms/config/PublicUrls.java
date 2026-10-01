package com.rentalert.admin_ms.config;

import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class PublicUrls {

    public static final String PREFIX = "/admin";
    public static final String AUTH_TAG = PREFIX + "/auth";
    public static final String AUDIT_LOG_TAG = PREFIX + "/audit-logs";
    public static final String CITY_TAG = PREFIX + "/cities";
    public static final String FEEDBACK_TAG = PREFIX + "/feedbacks";
    public static final String SCHEDULER_SETTINGS_TAG = PREFIX + "/scheduler-settings";

    public static final RequestMatcher PUBLIC_URLS = PathPatternRequestMatcher.withDefaults().matcher(AUTH_TAG + "/login");
}
