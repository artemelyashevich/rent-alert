package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.ModerationVerdict;

public interface TextModerationService {

    ModerationVerdict check(String text);
}
