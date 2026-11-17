package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.RoommateCalendarEventResponse;
import java.time.LocalDate;
import java.util.List;

public interface RoommateCalendarService {

    /**
     * Scheduled roommate viewings of the user whose date falls into [from, to], ordered by date. The range is
     * inclusive and at most 366 days; counterparty names are omitted if identity-ms is unavailable.
     */
    List<RoommateCalendarEventResponse> getEvents(Long userId, LocalDate from, LocalDate to);
}
