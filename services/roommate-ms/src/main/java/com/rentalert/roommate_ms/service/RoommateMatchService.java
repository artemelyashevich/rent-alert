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

    /**
     * Records interest in a partner candidate; mutual interest moves the match to MATCHED.
     * 409 if the match is already closed, 404 if the target has no roommate profile.
     */
    RoommateMatchView like(Long actorUserId, Long targetUserId);

    /**
     * Records interest in an active room listing; the listing author is the other side of the match.
     * 404 if the listing is missing, 400 if it is not ACTIVE.
     */
    RoommateMatchView likeListing(Long actorUserId, Long listingId);

    /**
     * Applies one action to a match the actor participates in. A stale {@code expectedVersion} gives 409;
     * a null version skips the check.
     */
    RoommateMatchView transition(
        Long matchId, Long actorUserId, RoommateMatchAction action,
        RoommateTransitionCommand command, Long expectedVersion);

    /**
     * The actor's matches, newest first; the counterparty's name and photo come from identity-ms and are
     * left empty if it is unavailable.
     */
    Page<RoommateMatchView> myMatches(Long userId, List<RoommateMatchStatus> statuses, Pageable pageable);

    /**
     * One match the requester participates in, 403 otherwise.
     */
    RoommateMatchView getMatch(Long matchId, Long requesterUserId);

    /**
     * The counterparty's contact; 403 until contacts are unlocked, which happens when the match reaches MATCHED.
     */
    RoommateContactResponse getContacts(Long matchId, Long requesterUserId);

    /**
     * Moves WAITING_RESPONSE matches past their deadline to EXPIRED and returns how many were expired.
     */
    int expireStaleMatches();
}
