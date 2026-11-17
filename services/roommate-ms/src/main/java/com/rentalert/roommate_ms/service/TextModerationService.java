package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.ModerationVerdict;

public interface TextModerationService {

    /**
     * Synchronous auto-filter for user texts: banned words ({@code app.moderation.banned-words}) are rejected, text
     * that looks like contact details goes to manual moderation, anything else is approved.
     */
    ModerationVerdict check(String text);
}
