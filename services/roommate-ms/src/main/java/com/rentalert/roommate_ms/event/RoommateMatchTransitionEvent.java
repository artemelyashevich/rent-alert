package com.rentalert.roommate_ms.event;

import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import java.util.List;

public record RoommateMatchTransitionEvent(
    Long matchId,
    RoommateScenario scenario,
    RoommateMatchStatus fromStatus,
    RoommateMatchStatus toStatus,
    Long actorUserId,
    List<Long> participantUserIds,
    Long counterpartyUserId,
    Long roommateViewingId
) {
}
