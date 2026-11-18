package com.rentalert.admin_ms.config;

import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class PublicUrls {

    public static final String PREFIX = "/admin";
    public static final String AUTH_TAG = PREFIX + "/auth";
    public static final String AUDIT_LOG_TAG = PREFIX + "/audit-logs";
    public static final String CITY_TAG = PREFIX + "/cities";
    public static final String FEEDBACK_TAG = PREFIX + "/feedbacks";
    public static final String JOB_SETTINGS_TAG = PREFIX + "/job-settings";
    public static final String LISTING_STATISTICS_TAG = PREFIX + "/listing-statistics";
    public static final String MIGRATION_TAG = PREFIX + "/migrations";
    public static final String NOTIFICATION_TAG = PREFIX + "/notifications";
    public static final String USER_STATISTICS_TAG = PREFIX + "/user-statistics";
    public static final String PROFILE_TAG = PREFIX + "/profiles";
    public static final String ROOMMATE_MODERATION_TAG = PREFIX + "/roommate/moderation";
    public static final String SCHEDULER_SETTINGS_TAG = PREFIX + "/scheduler-settings";

    public static final RequestMatcher PUBLIC_URLS = PathPatternRequestMatcher.withDefaults().matcher(AUTH_TAG + "/login");
}
