package com.rentalert.roommate_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.roommate_ms.domain.RoommateCalendarEventView;
import com.rentalert.roommate_ms.dto.RoommateCalendarEventResponse;
import com.rentalert.roommate_ms.repository.RoommateCalendarEventViewRepository;
import com.rentalert.roommate_ms.service.RoommateCalendarService;
import com.rentalert.roommate_ms.service.RoommateUserBriefs;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateCalendarServiceImpl implements RoommateCalendarService {

    private static final int MAX_RANGE_DAYS = 366;

    private final RoommateCalendarEventViewRepository calendarEventViewRepository;
    private final RoommateUserBriefs roommateUserBriefs;

    @Override
    @Transactional(readOnly = true)
    public List<RoommateCalendarEventResponse> getEvents(Long userId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(userId, "userId must not be null");
        validateRange(from, to);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<RoommateCalendarEventView> events = calendarEventViewRepository
            .findAllByIdUserIdAndViewingDateBetweenOrderByViewingDateAsc(userId, fromInstant, toInstant);
        Map<Long, String> usernames = findUsernamesOrEmpty(events.stream()
            .map(RoommateCalendarEventView::getCounterpartyUserId)
            .distinct()
            .toList());
        return events.stream()
            .map(event -> toResponse(event, usernames.get(event.getCounterpartyUserId())))
            .toList();
    }

    private static RoommateCalendarEventResponse toResponse(RoommateCalendarEventView event, String counterpartyUsername) {
        return new RoommateCalendarEventResponse(
            event.getMatchId(),
            event.getListingId(),
            event.getTitle(),
            event.getAddress(),
            event.getMetroStation(),
            event.getPrice(),
            event.getCurrency(),
            event.getCoverPhotoUrl(),
            event.getViewingDate(),
            event.getStatus(),
            event.getCounterpartyUserId(),
            counterpartyUsername);
    }

    private Map<Long, String> findUsernamesOrEmpty(Collection<Long> userIds) {
        return roommateUserBriefs.find(userIds).values().stream()
            .filter(brief -> brief.username() != null)
            .collect(Collectors.toMap(UserBriefResponse::userId, UserBriefResponse::username, (a, b) -> a));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new AppException(InternalExceptionCode.EX400_INVALID_DATE_RANGE, HttpStatus.BAD_REQUEST);
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new AppException(InternalExceptionCode.EX400_DATE_RANGE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }
    }
}
