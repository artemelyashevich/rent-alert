package com.rentalert.listing_ms.service;

import com.rentalert.listing_ms.dto.CalendarDaySummaryDto;
import com.rentalert.listing_ms.dto.CalendarEventDto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface CalendarService {

    List<CalendarEventDto> getEvents(Long userId, LocalDate from, LocalDate to);

    List<CalendarEventDto> getAllEvents(Long userId);

    List<CalendarDaySummaryDto> getMonthSummary(Long userId, YearMonth month);

    Optional<CalendarEventDto> getEvent(Long userId, Long listingId);
}
