package com.rentalert.identity_ms.controller.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.identity_ms.mapper.UserFilterMapper;
import com.rentalert.identity_ms.service.UserFilterService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @Test
    void returnsMatchFilter() {
        UserFilterResponse response = UserFilterResponse.builder().id(3L).active(true).build();
        when(userFilterService.findByOwnerMatchId(7L)).thenReturn(Optional.of(response));

        ResponseEntity<UserFilterResponse> result = controller.findMatchFilter(7L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void noContentWithoutMatchFilter() {
        when(userFilterService.findByOwnerMatchId(7L)).thenReturn(Optional.empty());

        assertThat(controller.findMatchFilter(7L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void upsertsMatchFilter() {
        UserFilterRequest request = UserFilterRequest.builder().city("Минск").build();
        UserFilterResponse response = UserFilterResponse.builder().id(3L).build();
        when(userFilterService.upsertForMatch(7L, request)).thenReturn(response);

        ResponseEntity<UserFilterResponse> result = controller.upsertMatchFilter(7L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void setsMatchFilterActivation() {
        UserFilterResponse response = UserFilterResponse.builder().id(3L).active(false).build();
        when(userFilterService.setActivationForMatch(7L, false)).thenReturn(response);

        ResponseEntity<UserFilterResponse> result = controller.setMatchFilterActivation(7L, false);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }
}
