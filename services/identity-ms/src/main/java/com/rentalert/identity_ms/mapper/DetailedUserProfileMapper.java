package com.rentalert.identity_ms.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalert.identity_ms.domain.DetailedUserProfileView;
import com.rentalert.identity_ms.dto.FeedbackResponse;
import com.rentalert.identity_ms.dto.filter.UserFilterResponse;
import com.rentalert.identity_ms.dto.profile.UserProfileResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DetailedUserProfileMapper {

    private static final TypeReference<List<UserFilterResponse>> FILTER_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<FeedbackResponse>> FEEDBACK_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public DetailedUserProfileMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UserProfileResponse toUserProfileResponse(DetailedUserProfileView src) {
        return new UserProfileResponse(
            src.getId(),
            src.getUsername(),
            src.getPhotoUrl(),
            src.getBotStatus(),
            src.getRoleName(),
            src.getLastLogin(),
            src.getQuietFrom(),
            src.getQuietTo(),
            src.getRegisteredAt(),
            src.getEmail(),
            null,
            null,
            null,
            parse(src.getActiveFiltersJson(), FILTER_TYPE, "active filters"),
            parse(src.getRecentFeedbacksJson(), FEEDBACK_TYPE, "recent feedbacks")
        );
    }

    private <T> List<T> parse(String json, TypeReference<List<T>> type, String what) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to parse " + what + " JSON from detailed_user_profiles view", e);
        }
    }
}
