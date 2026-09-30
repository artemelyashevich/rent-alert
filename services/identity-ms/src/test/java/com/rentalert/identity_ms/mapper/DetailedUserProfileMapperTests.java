package com.rentalert.identity_ms.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.identity_ms.domain.DetailedUserProfileView;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class DetailedUserProfileMapperTests {

    private final DetailedUserProfileMapper mapper = new DetailedUserProfileMapper(JsonMapper.builder().build());

    @Test
    void parsesFiltersAndFeedbacksAggregatedByTheView() {
        DetailedUserProfileView view = new DetailedUserProfileView();
        view.setId(5L);
        view.setUsername("ann");
        view.setActiveFiltersJson("""
            [{"id":1,"priceFrom":200,"priceTo":800,"roomsFrom":1,"roomsTo":3,"areaFrom":30.5,"areaTo":80,
              "active":true,"currency":"USD","createdAt":"2026-09-28T10:15:30.123456+00:00","updatedAt":"2026-09-28T10:15:30+00:00"}]
            """);
        view.setRecentFeedbacksJson("""
            [{"id":7,"userId":5,"username":"ann","message":"ok","rating":5,
              "createdAt":"2026-09-27T08:00:00+00:00","updatedAt":"2026-09-27T08:00:00+00:00"}]
            """);

        UserProfileResponse response = mapper.toUserProfileResponse(view);

        assertThat(response.userId()).isEqualTo(5L);
        assertThat(response.activeFilters()).singleElement().satisfies(filter -> {
            assertThat(filter.getPriceTo()).isEqualTo(800);
            assertThat(filter.isActive()).isTrue();
            assertThat(filter.getCreatedAt()).isEqualTo(Instant.parse("2026-09-28T10:15:30.123456Z"));
        });
        assertThat(response.recentFeedbacks()).singleElement().satisfies(feedback -> {
            assertThat(feedback.rating()).isEqualTo(5);
            assertThat(feedback.createdAt()).isEqualTo(Instant.parse("2026-09-27T08:00:00Z"));
        });
    }

    @Test
    void emptyAggregatesBecomeEmptyLists() {
        DetailedUserProfileView view = new DetailedUserProfileView();
        view.setId(5L);
        view.setActiveFiltersJson("[]");

        UserProfileResponse response = mapper.toUserProfileResponse(view);

        assertThat(response.activeFilters()).isEmpty();
        assertThat(response.recentFeedbacks()).isEmpty();
    }
}
