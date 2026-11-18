package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.dto.RoommateContactResponse;
import com.rentalert.roommate_ms.dto.RoommateMatchAction;
import com.rentalert.roommate_ms.dto.RoommateMatchView;
import com.rentalert.roommate_ms.dto.RoommateTransitionCommand;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoommateMatchService {

    RoommateMatchView like(Long actorUserId, Long targetUserId);

    RoommateMatchView likeListing(Long actorUserId, Long listingId);

    RoommateMatchView transition(
        Long matchId, Long actorUserId, RoommateMatchAction action,
        RoommateTransitionCommand command, Long expectedVersion);

    Page<RoommateMatchView> myMatches(Long userId, List<RoommateMatchStatus> statuses, Pageable pageable);

    RoommateMatchView getMatch(Long matchId, Long requesterUserId);

    RoommateContactResponse getContacts(Long matchId, Long requesterUserId);

    int expireStaleMatches();
}
