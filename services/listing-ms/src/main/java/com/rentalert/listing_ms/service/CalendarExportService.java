package com.rentalert.listing_ms.service;

import com.rentalert.listing_ms.dto.CalendarEventDto;

import java.util.List;

public interface CalendarExportService {

    byte[] toIcs(CalendarEventDto event);

    byte[] toIcs(List<CalendarEventDto> events);
}
