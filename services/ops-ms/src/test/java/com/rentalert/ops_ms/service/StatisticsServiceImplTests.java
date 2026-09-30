package com.rentalert.ops_ms.service;

import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.ops_ms.repository.UserStatisticsRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class StatisticsServiceImplTests {

    private static final ZoneId MINSK = ZoneId.of("Europe/Minsk");

    private final UserStatisticsRepository repository = mock(UserStatisticsRepository.class);
    private final StatisticsService service = new StatisticsServiceImpl(repository, MINSK);

    @Test
    void countsEventForTodayWithItsFilter() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.LISTING_RECEIVED).filterId(5L).build();
        event.setInitiatorId(10L);

        service.record(event);

        verify(repository).increment(10L, 5L, "LISTING_RECEIVED", LocalDate.now(MINSK));
    }

    @Test
    void skipsEventWithoutUser() {
        service.record(UserActionEvent.builder().event(UserEvent.FAVORITE_ADDED).build());

        verifyNoInteractions(repository);
    }
}
