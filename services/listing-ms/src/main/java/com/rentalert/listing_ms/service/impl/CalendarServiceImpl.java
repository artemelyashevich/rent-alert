package com.rentalert.listing_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.listing_ms.domain.CalendarEventView;
import com.rentalert.listing_ms.domain.FavoriteEmbeddedId;
import com.rentalert.listing_ms.dto.CalendarDaySummaryDto;
import com.rentalert.listing_ms.dto.CalendarEventDto;
import com.rentalert.listing_ms.mapper.CalendarEventMapper;
import com.rentalert.listing_ms.repository.CalendarEventViewRepository;
import com.rentalert.listing_ms.service.CalendarService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private static final int MAX_RANGE_DAYS = 366;

    private static final int MAX_MONTHS_LOOKBACK = 24;
    private static final int MAX_MONTHS_LOOKAHEAD = 24;

    private final CalendarEventViewRepository calendarEventViewRepository;
    private final CalendarEventMapper calendarEventMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getEvents(Long userId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(userId, "userId must not be null");
        validateRange(from, to);
        log.debug("Fetching calendar events for user {} from {} to {}", userId, from, to);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<CalendarEventView> events = calendarEventViewRepository
            .findAllByIdUserIdAndViewingDateBetweenOrderByViewingDateAsc(userId, fromInstant, toInstant);
        log.info("Found {} calendar events for user {} in range [{}, {}]", events.size(), userId, from, to);
        return events.stream()
            .map(calendarEventMapper::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getAllEvents(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        log.debug("Fetching all calendar events for user {}", userId);
        List<CalendarEventView> events = calendarEventViewRepository
            .findAllByIdUserIdOrderByViewingDateAsc(userId);
        log.info("Found {} total calendar events for user {}", events.size(), userId);
        return events.stream()
            .map(calendarEventMapper::toDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarDaySummaryDto> getMonthSummary(Long userId, YearMonth month) {
        Objects.requireNonNull(userId, "userId must not be null");
        validateMonth(month);
        List<CalendarEventDto> events = getEvents(userId, month.atDay(1), month.atEndOfMonth());
        return events.stream()
            .collect(Collectors.groupingBy(
                event -> event.viewingDate().atZone(ZoneOffset.UTC).toLocalDate(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .map(entry -> new CalendarDaySummaryDto(entry.getKey(), entry.getValue().intValue()))
            .sorted(Comparator.comparing(CalendarDaySummaryDto::date))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CalendarEventDto> getEvent(Long userId, Long listingId) {
        Objects.requireNonNull(userId, "userId must not be null");
        FavoriteEmbeddedId id = new FavoriteEmbeddedId(userId, listingId);
        return calendarEventViewRepository.findById(id)
            .map(calendarEventMapper::toDto);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new AppException(InternalExceptionCode.EX400_INVALID_DATE_RANGE, HttpStatus.BAD_REQUEST);
        }
        if (from.isAfter(to)) {
            log.warn("Invalid calendar range requested: from={} is after to={}", from, to);
            throw new AppException(InternalExceptionCode.EX400_INVALID_DATE_RANGE, HttpStatus.BAD_REQUEST);
        }
        long spanDays = ChronoUnit.DAYS.between(from, to);
        if (spanDays > MAX_RANGE_DAYS) {
            log.warn("Calendar range too large: {} days requested (max {})", spanDays, MAX_RANGE_DAYS);
            throw new AppException(InternalExceptionCode.EX400_DATE_RANGE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }
    }

    private void validateMonth(YearMonth month) {
        if (month == null) {
            throw new AppException(InternalExceptionCode.EX400_INVALID_DATE_RANGE, HttpStatus.BAD_REQUEST);
        }
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        YearMonth min = now.minusMonths(MAX_MONTHS_LOOKBACK);
        YearMonth max = now.plusMonths(MAX_MONTHS_LOOKAHEAD);
        if (month.isBefore(min) || month.isAfter(max)) {
            log.warn("Requested month {} is outside allowed window [{}, {}]", month, min, max);
            throw new AppException(InternalExceptionCode.EX400_MONTH_OUT_OF_RANGE, HttpStatus.BAD_REQUEST);
        }
    }
}
