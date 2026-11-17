package com.rentalert.roommate_ms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.service.UserSessionService;
import com.rentalert.roommate_ms.domain.RoommateDecision;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import com.rentalert.roommate_ms.dto.RoommateMatchView;
import com.rentalert.roommate_ms.service.RoommateFeedService;
import com.rentalert.roommate_ms.service.RoommateHiddenService;
import com.rentalert.roommate_ms.service.RoommateMatchService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoommateFeedControllerTests {

    private final RoommateFeedService feedService = mock(RoommateFeedService.class);
    private final RoommateHiddenService hiddenService = mock(RoommateHiddenService.class);
    private final RoommateMatchService matchService = mock(RoommateMatchService.class);
    private final UserSessionService userSessionService = mock(UserSessionService.class);

    private final RoommateFeedController controller =
        new RoommateFeedController(feedService, hiddenService, matchService, userSessionService);

    private void currentUserIs(long userId) {
        when(userSessionService.getCurrentUser()).thenReturn(UserSession.builder().userId(userId).build());
    }

    private static RoommateMatchView view() {
        return new RoommateMatchView(100L, RoommateScenario.PARTNER, RoommateMatchStatus.WAITING_RESPONSE, 0L, 9L,
            null, null, null, RoommateDecision.INTERESTED, RoommateDecision.PENDING, true, false, false, false,
            null, null, null, null, Instant.now(), Instant.now());
    }

    @Test
    void likeCandidateActsAsTheCurrentUser() {
        currentUserIs(7L);
        RoommateMatchView expected = view();
        when(matchService.like(7L, 9L)).thenReturn(expected);

        assertThat(controller.likeCandidate(9L)).isEqualTo(expected);
    }

    @Test
    void likeListingActsAsTheCurrentUser() {
        currentUserIs(7L);
        RoommateMatchView expected = view();
        when(matchService.likeListing(7L, 15L)).thenReturn(expected);

        assertThat(controller.likeListing(15L)).isEqualTo(expected);
    }

    @Test
    void skipCandidateHidesThroughHiddenService() {
        currentUserIs(7L);

        controller.skipCandidate(9L);

        verify(hiddenService).skipCandidate(7L, 9L);
        verifyNoInteractions(matchService);
    }
}
