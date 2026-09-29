package com.rentalert.identity_ms.controller.internal;

import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.identity_ms.mapper.UserFilterMapper;
import com.rentalert.identity_ms.service.UserFilterService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserFilterInternalControllerTests {

    private final UserFilterService userFilterService = mock(UserFilterService.class);
    private final UserFilterMapper userFilterMapper = mock(UserFilterMapper.class);
    private final UserFilterInternalController controller =
        new UserFilterInternalController(userFilterService, userFilterMapper);

    @Test
    void returnsActiveFilter() {
        UserFilter filter = new UserFilter();
        UserFilterResponse response = UserFilterResponse.builder().id(3L).active(true).build();
        when(userFilterService.findActiveFilterByUserId(42L)).thenReturn(filter);
        when(userFilterMapper.toResponse(filter)).thenReturn(response);

        ResponseEntity<UserFilterResponse> result = controller.findActiveFilter(42L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void noContentWithoutActiveFilter() {
        assertThat(controller.findActiveFilter(42L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
