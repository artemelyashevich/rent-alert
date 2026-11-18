package com.rentalert.roommate_ms.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.roommate.RoommateMatchParticipantsDto;
import com.rentalert.dto.roommate.RoommateMatchStatus;
import com.rentalert.roommate_ms.domain.RoommateMatch;
import com.rentalert.roommate_ms.repository.RoommateMatchRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoommateMatchInternalControllerTests {

    private final RoommateMatchRepository matchRepository = mock(RoommateMatchRepository.class);
    private final RoommateMatchInternalController controller = new RoommateMatchInternalController(matchRepository);

    @Test
    void returnsParticipantsStatusAndUnlockTimeForEveryFoundMatch() {
        Instant unlockedAt = Instant.parse("2026-10-01T10:00:00Z");
        RoommateMatch matched = RoommateMatch.builder()
            .userLoId(1L)
            .userHiId(2L)
            .status(com.rentalert.roommate_ms.domain.RoommateMatchStatus.MATCHED)
            .contactsUnlockedAt(unlockedAt)
            .build();
        matched.setId(10L);
        RoommateMatch interested = RoommateMatch.builder()
            .userLoId(3L)
            .userHiId(4L)
            .status(com.rentalert.roommate_ms.domain.RoommateMatchStatus.INTERESTED)
            .build();
        interested.setId(11L);
        when(matchRepository.findAllById(List.of(10L, 11L, 12L))).thenReturn(List.of(matched, interested));

        List<RoommateMatchParticipantsDto> response = controller.findParticipants(List.of(10L, 11L, 12L));

        assertThat(response).containsExactly(
            new RoommateMatchParticipantsDto(10L, RoommateMatchStatus.MATCHED, 1L, 2L, unlockedAt),
            new RoommateMatchParticipantsDto(11L, RoommateMatchStatus.INTERESTED, 3L, 4L, null));
    }
}
