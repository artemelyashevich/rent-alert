package com.rentalert.redis.dto;

import com.rentalert.dto.auth.EmailOtp;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.dto.user.UserSession;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;

/**
 * A cache and the type of its values. {@link #name()} is the Redis key prefix: {@code USER_SESSION:<token>}.
 */
@Getter
@RequiredArgsConstructor
public enum CacheType {

    USER_PROFILE(new TypeReference<UserProfileResponse>() {
    }),
    USER_SESSION(new TypeReference<UserSession>() {
    }),
    USER_ACTIVE_TOKEN(new TypeReference<String>() {
    }),
    /** Key: OTP request id. */
    EMAIL_OTP(new TypeReference<EmailOtp>() {
    }),
    /** Key: userId. Profile as returned by identity-ms internal API. */
    USER_PROFILE_INTERNAL(new TypeReference<UserProfileResponse>() {
    }),
    /** Key: userId. */
    USER_ACTIVE_FILTER(new TypeReference<UserFilterResponse>() {
    }),

    /** Key: {@code ScheduleProvider}. */
    SCHEDULER_SETTINGS(new TypeReference<SchedulerSettingsResponse>() {
    }),
    /** Key: {@code CurrencyPair}. */
    DAILY_CURRENCY_RATE(new TypeReference<CurrencyRateDto>() {
    }),
    CITY_NAMES(new TypeReference<List<String>>() {
    }),
    /** Key: parser task key; marks a task that is being processed right now. */
    SCHEDULER_IN_FLIGHT(new TypeReference<Boolean>() {
    }),
    /** Redis sets of task keys already processed by the parsers of a provider. */
    KUFAR_PROCESSED(new TypeReference<String>() {
    }),
    ONLINER_PROCESSED(new TypeReference<String>() {
    }),
    REALT_PROCESSED(new TypeReference<String>() {
    });

    private final TypeReference<?> type;
}
