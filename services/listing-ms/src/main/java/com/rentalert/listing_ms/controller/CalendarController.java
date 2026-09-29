package com.rentalert.listing_ms.controller;

import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.listing_ms.dto.CalendarDaySummaryDto;
import com.rentalert.listing_ms.dto.CalendarEventDto;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.exception.AppException;
import com.rentalert.listing_ms.service.CalendarExportService;
import com.rentalert.listing_ms.service.CalendarService;
import com.rentalert.redis.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Calendar", description = "Календарь запланированных просмотров")
@RestController
@RequestMapping(PublicUrls.CALENDAR_TAG)
@RequiredArgsConstructor
@Validated
public class CalendarController {

    private static final String SUPPORTED_EXPORT_FORMAT = "ics";

    private final CalendarService calendarService;
    private final CalendarExportService calendarExportService;
    private final UserSessionService userSessionService;

    @Operation(
            summary = "Получить запланированные просмотры за период",
            description = "Возвращает записи избранного, отображаемые в календаре " +
                    "(статус помечен shows_in_calendar=true), у которых viewing_date " +
                    "попадает в диапазон [from, to]. Максимальный диапазон - 366 дней."
    )
    @GetMapping("/events")
    public List<CalendarEventDto> getEvents(
            @Parameter(description = "Начало диапазона (включительно)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Конец диапазона (включительно)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return calendarService.getEvents(userId, from, to);
    }

    @Operation(
            summary = "Получить количество просмотров по дням месяца",
            description = "Используется для точек-маркеров в сетке календаря. " +
                    "Допустимый диапазон - ±24 месяца от текущего."
    )
    @GetMapping("/summary")
    public List<CalendarDaySummaryDto> getMonthSummary(
            @Parameter(description = "Месяц в формате yyyy-MM")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return calendarService.getMonthSummary(userId, month);
    }

    @Operation(
            summary = "Экспортировать один просмотр в формате .ics",
            description = "Отдаёт файл события просмотра для импорта в Apple Calendar/Outlook/Google Calendar."
    )
    @GetMapping(value = "/events/{listingId}/export", produces = "text/calendar")
    public ResponseEntity<byte[]> exportOne(
            @Parameter(description = "ID объявления")
            @PathVariable @Positive Long listingId,

            @Parameter(description = "Формат экспорта. На данный момент поддерживается только ics")
            @RequestParam(required = false, defaultValue = SUPPORTED_EXPORT_FORMAT) String format
    ) {
        validateFormat(format);
        Long userId = userSessionService.getCurrentUser().getUserId();

        CalendarEventDto event = calendarService.getEvent(userId, listingId)
                .orElseThrow(() -> new AppException(
                        InternalExceptionCode.EX404_FAVORITE_NOT_FOUND, HttpStatus.NOT_FOUND));

        byte[] ics = calendarExportService.toIcs(event);
        return icsResponse(ics, "viewing-" + listingId + ".ics");
    }

    @Operation(
            summary = "Экспортировать все запланированные просмотры одним файлом",
            description = "Если from/to не переданы - экспортируются все календарные записи пользователя. " +
                    "Если переданы - оба сразу, иначе это ошибка запроса. " +
                    "Если подходящих записей нет - 404."
    )
    @GetMapping(value = "/export", produces = "text/calendar")
    public ResponseEntity<byte[]> exportAll(
            @Parameter(description = "Формат экспорта. На данный момент поддерживается только ics")
            @RequestParam(required = false, defaultValue = SUPPORTED_EXPORT_FORMAT) String format,

            @Parameter(description = "Начало диапазона. Указывается только вместе с to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Конец диапазона. Указывается только вместе с from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        validateFormat(format);
        Long userId = userSessionService.getCurrentUser().getUserId();

        List<CalendarEventDto> events = resolveEventsForExport(userId, from, to);

        if (events.isEmpty()) {
            throw new AppException(InternalExceptionCode.EX404_NO_CALENDAR_EVENTS, HttpStatus.NOT_FOUND);
        }

        byte[] ics = calendarExportService.toIcs(events);
        return icsResponse(ics, "viewings.ics");
    }

    private List<CalendarEventDto> resolveEventsForExport(Long userId, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return calendarService.getAllEvents(userId);
        }
        if (from != null && to != null) {
            return calendarService.getEvents(userId, from, to);
        }
        throw new AppException(InternalExceptionCode.EX400_INVALID_DATE_RANGE, HttpStatus.BAD_REQUEST);
    }

    private void validateFormat(String format) {
        if (!SUPPORTED_EXPORT_FORMAT.equalsIgnoreCase(format)) {
            throw new AppException(InternalExceptionCode.EX400_UNSUPPORTED_EXPORT_FORMAT, HttpStatus.BAD_REQUEST);
        }
    }

    private ResponseEntity<byte[]> icsResponse(byte[] ics, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(ics);
    }
}
