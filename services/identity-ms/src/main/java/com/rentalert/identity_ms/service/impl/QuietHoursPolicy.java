package com.rentalert.identity_ms.service.impl;

import com.rentalert.identity_ms.domain.UserSettings;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QuietHoursPolicy {

    private final ZoneId botZone;

    public QuietHoursPolicy(@Value("${app.bot.timezone:Europe/Minsk}") ZoneId botZone) {
        this.botZone = botZone;
    }

    public Optional<Instant> resumeAt(UserSettings settings, Instant at) {
        return isQuietAt(settings, at) ? Optional.of(resumeAfter(settings.getQuietTo(), at)) : Optional.empty();
    }

    private boolean isQuietAt(UserSettings settings, Instant at) {
        LocalTime from = settings.getQuietFrom();
        LocalTime to = settings.getQuietTo();
        if (from == null || to == null || from.equals(to)) {
            return false;
        }
        LocalTime time = at.atZone(botZone).toLocalTime();
        if (from.isBefore(to)) {
            return !time.isBefore(from) && time.isBefore(to);
        }
        return !time.isBefore(from) || time.isBefore(to);
    }

    private Instant resumeAfter(LocalTime quietTo, Instant at) {
        ZonedDateTime reference = at.atZone(botZone);
        LocalDate resumeDate = reference.toLocalTime().isBefore(quietTo)
            ? reference.toLocalDate()
            : reference.toLocalDate().plusDays(1);
        return ZonedDateTime.of(resumeDate, quietTo, botZone).toInstant();
    }
}
