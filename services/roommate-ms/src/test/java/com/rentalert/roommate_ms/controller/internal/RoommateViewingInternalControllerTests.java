package com.rentalert.roommate_ms.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.roommate.RoommateViewingDto;
import com.rentalert.dto.roommate.RoommateViewingStatus;
import com.rentalert.roommate_ms.domain.RoommateMatch;
import com.rentalert.roommate_ms.domain.RoommateViewing;
import com.rentalert.roommate_ms.repository.RoommateMatchRepository;
import com.rentalert.roommate_ms.repository.RoommateViewingRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RoommateViewingInternalControllerTests {

    private final RoommateMatchRepository matchRepository = mock(RoommateMatchRepository.class);
    private final RoommateViewingRepository viewingRepository = mock(RoommateViewingRepository.class);
    private final RoommateViewingInternalController controller =
        new RoommateViewingInternalController(matchRepository, viewingRepository);

    @Test
    void returnsViewingWithMatchParticipantsAndListingSnapshot() {
        RoommateMatch match = RoommateMatch.builder().userLoId(1L).userHiId(2L).build();
        match.setId(10L);
        RoommateViewing viewing = RoommateViewing.builder()
            .match(match)
            .listingId(15L)
            .address("Немига 5, кв. 3")
            .viewingDate(Instant.parse("2026-10-05T12:00:00Z"))
            .createdByUserId(1L)
            .status(com.rentalert.roommate_ms.domain.RoommateViewingStatus.SCHEDULED)
            .listingTitle("2-комн.")
            .listingStreet("Немига 5")
            .build();
        viewing.setId(5L);
        when(viewingRepository.findById(5L)).thenReturn(Optional.of(viewing));
        when(matchRepository.findById(10L)).thenReturn(Optional.of(match));

        ResponseEntity<RoommateViewingDto> response = controller.findViewing(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new RoommateViewingDto(
            5L, 10L, 1L, 2L, Instant.parse("2026-10-05T12:00:00Z"), RoommateViewingStatus.SCHEDULED,
            15L, "Немига 5, кв. 3", "2-комн.", "Немига 5"));
    }

    @Test
    void returnsNoContentWhenViewingMissing() {
        when(viewingRepository.findById(6L)).thenReturn(Optional.empty());

        ResponseEntity<RoommateViewingDto> response = controller.findViewing(6L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }
}
