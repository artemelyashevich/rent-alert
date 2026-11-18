package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.RoommateCalendarEventResponse;
import java.time.LocalDate;
import java.util.List;

public interface RoommateCalendarService {

    List<RoommateCalendarEventResponse> getEvents(Long userId, LocalDate from, LocalDate to);
}
