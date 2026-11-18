package com.rentalert.roommate_ms.controller.internal;

import com.rentalert.dto.roommate.RoommateMatchParticipantsDto;
import com.rentalert.dto.roommate.RoommateMatchStatus;
import com.rentalert.roommate_ms.config.PublicUrls;
import com.rentalert.roommate_ms.repository.RoommateMatchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.PREFIX + PublicUrls.INTERNAL_PREFIX + "/matches")
@RequiredArgsConstructor
public class RoommateMatchInternalController {

    private final RoommateMatchRepository matchRepository;

    @PostMapping("/participants")
    public List<RoommateMatchParticipantsDto> findParticipants(@RequestBody List<Long> matchIds) {
        return matchRepository.findAllById(matchIds).stream()
            .map(match -> new RoommateMatchParticipantsDto(
                match.getId(),
                RoommateMatchStatus.valueOf(match.getStatus().name()),
                match.getUserLoId(),
                match.getUserHiId(),
                match.getContactsUnlockedAt()))
            .toList();
    }
}
