package com.rentalert.dto.profile;


import com.rentalert.dto.profile.filter.UserFilterResponse;
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
