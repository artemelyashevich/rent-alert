package com.rentalert.roommate_ms.controller;

import com.rentalert.redis.service.UserSessionService;
import com.rentalert.roommate_ms.config.PublicUrls;
import com.rentalert.roommate_ms.dto.RoommateCalendarEventResponse;
import com.rentalert.roommate_ms.service.RoommateCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Roommate calendar", description = "Запланированные просмотры пары текущего пользователя")
@RestController
@RequestMapping(PublicUrls.CALENDAR_TAG)
@RequiredArgsConstructor
public class RoommateCalendarController {

    private final RoommateCalendarService calendarService;
    private final UserSessionService userSessionService;

    @Operation(summary = "Календарь просмотров пары", description = "Диапазон [from, to] включительно, не более 366 дней.")
    @GetMapping
    public List<RoommateCalendarEventResponse> getEvents(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return calendarService.getEvents(userSessionService.getCurrentUser().getUserId(), from, to);
    }
}
