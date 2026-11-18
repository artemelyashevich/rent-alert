package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.redis.service.CacheService;
import com.rentalert.roommate_ms.domain.RoommateCalendarEventEmbeddedId;
import com.rentalert.roommate_ms.domain.RoommateCalendarEventView;
import com.rentalert.roommate_ms.domain.RoommateViewingStatus;
import com.rentalert.roommate_ms.repository.RoommateCalendarEventViewRepository;
import com.rentalert.roommate_ms.service.impl.RoommateCalendarServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoommateCalendarServiceImplTests {

    private static final Long USER_ID = 5L;
    private static final LocalDate FROM = LocalDate.of(2030, 1, 1);
    private static final LocalDate TO = LocalDate.of(2030, 1, 31);

    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final RoommateUserBriefs roommateUserBriefs =
        new RoommateUserBriefs(identityMsService, new RoommateBatchCache(mock(CacheService.class)));
    private final RoommateCalendarEventViewRepository repository = mock(RoommateCalendarEventViewRepository.class);
    private final RoommateCalendarService service = new RoommateCalendarServiceImpl(repository, roommateUserBriefs);

    @Test
    void eventsCarryCounterpartyName() {
        when(repository.findAllByIdUserIdAndViewingDateBetweenOrderByViewingDateAsc(eq(USER_ID), any(), any()))
            .thenReturn(List.of(event()));
        when(identityMsService.findUserBriefs(anyList()))
            .thenReturn(List.of(new UserBriefResponse(6L, "roommate", null)));

        var result = service.getEvents(USER_ID, FROM, TO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Flat");
        assertThat(result.get(0).counterpartyUserId()).isEqualTo(6L);
        assertThat(result.get(0).counterpartyUsername()).isEqualTo("roommate");
    }

    @Test
    void eventsAreReturnedWithoutNamesWhenIdentityFails() {
        when(repository.findAllByIdUserIdAndViewingDateBetweenOrderByViewingDateAsc(eq(USER_ID), any(), any()))
            .thenReturn(List.of(event()));
        when(identityMsService.findUserBriefs(anyList())).thenThrow(new RuntimeException("identity-ms unavailable"));

        var result = service.getEvents(USER_ID, FROM, TO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).counterpartyUsername()).isNull();
    }

    @Test
    void reversedRangeIsRejected() {
        assertThatThrownBy(() -> service.getEvents(USER_ID, TO, FROM))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX400_INVALID_DATE_RANGE));
    }

    @Test
    void rangeLongerThanYearIsRejected() {
        assertThatThrownBy(() -> service.getEvents(USER_ID, FROM, FROM.plusDays(400)))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX400_DATE_RANGE_TOO_LARGE));
    }

    private static RoommateCalendarEventView event() {
        RoommateCalendarEventView view = new RoommateCalendarEventView();
        view.setId(new RoommateCalendarEventEmbeddedId(USER_ID, 9L));
        view.setMatchId(7L);
        view.setListingId(11L);
        view.setTitle("Flat");
        view.setPrice(BigDecimal.TEN);
        view.setCurrency(Currency.USD);
        view.setViewingDate(Instant.parse("2030-01-10T10:00:00Z"));
        view.setStatus(RoommateViewingStatus.SCHEDULED);
        view.setCounterpartyUserId(6L);
        return view;
    }
}
