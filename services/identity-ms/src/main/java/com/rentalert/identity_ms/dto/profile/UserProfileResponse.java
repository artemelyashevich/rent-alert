package com.rentalert.identity_ms.dto.profile;

import com.rentalert.identity_ms.domain.BotStatus;
import com.rentalert.identity_ms.domain.ColorTheme;
import com.rentalert.identity_ms.domain.SupportedLanguage;
import com.rentalert.identity_ms.domain.UserRole;
import com.rentalert.identity_ms.dto.FeedbackResponse;
import com.rentalert.identity_ms.dto.filter.UserFilterResponse;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public record UserProfileResponse(
    Long userId, String username, String photoUrl, BotStatus botStatus, UserRole roleName, Instant lastLogin,
    LocalTime quietFrom, LocalTime quietTo, Instant registeredAt,
    String email, String phoneNumber, ColorTheme colorTheme, SupportedLanguage language,
    List<UserFilterResponse> activeFilters, List<FeedbackResponse> recentFeedbacks
) {
}
