package com.rentalert.roommate_ms.controller.internal;

import com.rentalert.dto.roommate.RoommateViewingDto;
import com.rentalert.dto.roommate.RoommateViewingStatus;
import com.rentalert.roommate_ms.config.PublicUrls;
import com.rentalert.roommate_ms.domain.RoommateMatch;
import com.rentalert.roommate_ms.domain.RoommateViewing;
import com.rentalert.roommate_ms.repository.RoommateMatchRepository;
import com.rentalert.roommate_ms.repository.RoommateViewingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.PREFIX + PublicUrls.INTERNAL_PREFIX + "/viewings")
@RequiredArgsConstructor
public class RoommateViewingInternalController {

    private final RoommateMatchRepository matchRepository;
    private final RoommateViewingRepository viewingRepository;

    /** Просмотр пары для напоминаний; 204, если просмотра с таким id нет. */
    @GetMapping("/{viewingId}")
    public ResponseEntity<RoommateViewingDto> findViewing(@PathVariable Long viewingId) {
        return viewingRepository.findById(viewingId)
            .map(viewing -> ResponseEntity.ok(toDto(viewing)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private RoommateViewingDto toDto(RoommateViewing viewing) {
        RoommateMatch match = matchRepository.findById(viewing.getMatch().getId()).orElseThrow();
        return new RoommateViewingDto(
            viewing.getId(),
            match.getId(),
            match.getUserLoId(),
            match.getUserHiId(),
            viewing.getViewingDate(),
            RoommateViewingStatus.valueOf(viewing.getStatus().name()),
            viewing.getListingId(),
            viewing.getAddress(),
            viewing.getListingTitle(),
            viewing.getListingStreet()
        );
    }
}
