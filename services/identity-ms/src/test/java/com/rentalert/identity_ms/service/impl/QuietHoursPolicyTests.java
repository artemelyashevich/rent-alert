package com.rentalert.identity_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.identity_ms.domain.UserSettings;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class QuietHoursPolicyTests {

    private final QuietHoursPolicy policy = new QuietHoursPolicy(ZoneId.of("Europe/Minsk"));

    private static UserSettings quiet(String from, String to) {
        return UserSettings.builder().quietFrom(LocalTime.parse(from)).quietTo(LocalTime.parse(to)).build();
    }

    private static Instant minsk(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(ZoneId.of("Europe/Minsk")).toInstant();
    }

    @Test
    void overnightWindowResumesNextMorning() {
        UserSettings settings = quiet("23:00", "08:00");

        assertThat(policy.resumeAt(settings, minsk("2026-09-30T23:30:00")))
            .contains(minsk("2026-10-01T08:00:00"));
    }

    @Test
    void overnightWindowAfterMidnightResumesSameMorning() {
        UserSettings settings = quiet("23:00", "08:00");

        assertThat(policy.resumeAt(settings, minsk("2026-10-01T02:00:00")))
            .contains(minsk("2026-10-01T08:00:00"));
    }

    @Test
    void daytimeWindowResumesSameDay() {
        UserSettings settings = quiet("13:00", "15:00");

        assertThat(policy.resumeAt(settings, minsk("2026-09-30T14:00:00")))
            .contains(minsk("2026-09-30T15:00:00"));
    }

    @Test
    void sendsRightAwayOutsideTheWindow() {
        UserSettings settings = quiet("23:00", "08:00");

        assertThat(policy.resumeAt(settings, minsk("2026-09-30T12:00:00"))).isEmpty();
        assertThat(policy.resumeAt(settings, minsk("2026-10-01T08:00:00"))).isEmpty();
    }

    @Test
    void noQuietHoursWhenUnsetOrEmpty() {
        assertThat(policy.resumeAt(UserSettings.builder().build(), minsk("2026-09-30T23:30:00"))).isEmpty();
        assertThat(policy.resumeAt(quiet("08:00", "08:00"), minsk("2026-09-30T08:30:00"))).isEmpty();
    }
}
