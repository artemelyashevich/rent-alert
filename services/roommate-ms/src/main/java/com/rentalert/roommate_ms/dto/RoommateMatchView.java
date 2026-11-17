package com.rentalert.roommate_ms.dto;

import com.rentalert.roommate_ms.domain.RoommateDecision;
import com.rentalert.roommate_ms.domain.RoommateMatchStatus;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import com.rentalert.roommate_ms.domain.RoommateViewingStatus;
import java.time.Instant;

public record RoommateMatchView(
    Long id,
    RoommateScenario scenario,
    RoommateMatchStatus status,
    long statusVersion,
    Long counterpartyUserId,
    String counterpartyUsername,
    String counterpartyPhotoUrl,
    Long roommateListingId,
    RoommateDecision myDecision,
    RoommateDecision counterpartyDecision,
    boolean initiatedByMe,
    boolean contactsUnlocked,
    boolean iConfirmedMoveIn,
    boolean counterpartyConfirmedMoveIn,
    Long activeViewingId,
    Instant activeViewingDate,
    RoommateViewingStatus activeViewingStatus,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt
) {
}
