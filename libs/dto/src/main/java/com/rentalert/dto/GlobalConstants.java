package com.rentalert.dto;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GlobalConstants {

    public static final String SCHEDULER = "scheduler";
    public static final String SCHEDULER_CACHE_ATTEMPT = "scheduler-attempt";
    public static final String SCHEDULER_CACHE = "active-scheduler";
    public static final String DAILY_RATE_CACHE = "daily-currency-rate";
    public static final String SYNC_LOCK = "sync-lock-%s";
    public static final String SCHEDULER_PARSER = "schedulerParser";
    public static final String SCHEDULER_SYNC = "schedulerSync";
    public static final String USER_SESSION = "user_session:";
    public static final String SCHEDULER_CLEANUP = "CLEANUP";
    public static final String EVENT_EXECUTOR = "event-executor";
    public static final String BROADCAST_EXECUTOR = "broadcast-executor";
    public static final String USER_ACTIVE_TOKEN = "user_active_token";
    public static final String ADMIN = "ADMIN";
    public static final String KUFAR_IMG_PREFIX = "https://rms8.kufar.by/v1/gallery/";
    public static final String REALT_URL_PREFIX = "https://realt.by/rent-flat-for-long/object/";
    public static final String MAINTENANCE_LOCK = "MAINTENANCE_LOCK";
    public static final String OTP_CACHE = "otp";
    public static final String LOGIN_TYPE = "type";
    public static final String ROOMMATE_SCORE_CACHE = "roommate-score";
    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
}
