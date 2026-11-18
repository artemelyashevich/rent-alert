package com.rentalert.identity_ms.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.identity_ms.mapper.UserProfileMapper;
import com.rentalert.identity_ms.service.UserProfileService;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserProfileInternalControllerTests {

    private final UserProfileService userProfileService = mock(UserProfileService.class);
    private final UserProfileInternalController controller =
        new UserProfileInternalController(mock(UserProfileMapper.class), userProfileService);

    @Test
    void usernameSearchDelegatesToService() {
        List<UserBriefResponse> briefs = List.of(new UserBriefResponse(5L, "anna", null));
        when(userProfileService.searchBriefsByUsername("ann")).thenReturn(briefs);

        assertThat(controller.searchByUsername("ann")).isSameAs(briefs);
    }
}
