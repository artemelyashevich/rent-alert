package com.rentalert.listing_ms.service.impl;

import static com.rentalert.common.exception.InternalExceptionCode.EX400;

import com.rentalert.common.exception.AppException;
import com.rentalert.listing_ms.dto.CalendarEventDto;
import com.rentalert.listing_ms.service.CalendarExportService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CalendarExportServiceImpl implements CalendarExportService {

    private static final Duration DEFAULT_VIEWING_DURATION = Duration.ofHours(1);
    private static final DateTimeFormatter ICS_INSTANT_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    @Override
    public byte[] toIcs(CalendarEventDto event) {
        Objects.requireNonNull(event, "event must not be null");
        validateHasViewingDate(event);
        String ics = wrapCalendar(buildVEvent(event));
        return ics.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] toIcs(List<CalendarEventDto> events) {
        if (events == null || events.isEmpty()) {
            throw new AppException(EX400, HttpStatus.BAD_REQUEST);
        }
        events.forEach(this::validateHasViewingDate);
        StringBuilder vevents = new StringBuilder();
        for (CalendarEventDto event : events) {
            vevents.append(buildVEvent(event));
        }
        String ics = wrapCalendar(vevents.toString());
        return ics.getBytes(StandardCharsets.UTF_8);
    }

    private void validateHasViewingDate(CalendarEventDto event) {
        if (event.viewingDate() == null) {
            throw new AppException(EX400, HttpStatus.BAD_REQUEST);
        }
    }

    private String wrapCalendar(String vevents) {
        return """
            BEGIN:VCALENDAR
            VERSION:1.0
            PRODID:-//RentAlert//Viewing Export//RU
            CALSCALE:GREGORIAN
            METHOD:PUBLISH
            %sEND:VCALENDAR
            """.formatted(vevents);
    }

    private String buildVEvent(CalendarEventDto event) {
        Instant start = event.viewingDate();
        Instant end = start.plus(DEFAULT_VIEWING_DURATION);

        return """
            BEGIN:VEVENT
            UID:favorite-%s@renalert
            DTSTAMP:%s
            DTSTART:%s
            DTEND:%s
            SUMMARY:%s
            LOCATION:%s
            DESCRIPTION:%s
            END:VEVENT
            """.formatted(
            event.listingId(),
            ICS_INSTANT_FORMAT.format(Instant.now()),
            ICS_INSTANT_FORMAT.format(start),
            ICS_INSTANT_FORMAT.format(end),
            escape(event.title()),
            escape(event.address()),
            escape(buildDescription(event))
        );
    }

    private String buildDescription(CalendarEventDto event) {
        if (event.price() == null) {
            return "Просмотр объявления в RentAlert";
        }
        return "Просмотр объявления. Цена: " + event.price() + " " + event.currency();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n");
    }
}
