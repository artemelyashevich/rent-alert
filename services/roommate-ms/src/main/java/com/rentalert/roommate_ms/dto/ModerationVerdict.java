package com.rentalert.roommate_ms.dto;

import com.rentalert.dto.roommate.ModerationStatus;

public record ModerationVerdict(ModerationStatus status, String reason) {

    public static ModerationVerdict approved() {
        return new ModerationVerdict(ModerationStatus.APPROVED_AUTO, null);
    }

    public static ModerationVerdict pending(String reason) {
        return new ModerationVerdict(ModerationStatus.PENDING_MANUAL, reason);
    }

    public static ModerationVerdict rejected(String reason) {
        return new ModerationVerdict(ModerationStatus.REJECTED_AUTO, reason);
    }

    public boolean isPending() {
        return status == ModerationStatus.PENDING_MANUAL;
    }

    public boolean isRejected() {
        return status == ModerationStatus.REJECTED_AUTO || status == ModerationStatus.REJECTED;
    }
}
