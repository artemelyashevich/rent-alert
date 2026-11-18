package com.rentalert.admin_ms.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UserBriefsTests {

    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final UserBriefs userBriefs = new UserBriefs(identityMsService);

    @Test
    void usernameSearchReturnsMatchedUserIds() {
        when(identityMsService.searchUserBriefs("ann")).thenReturn(List.of(
            new UserBriefResponse(5L, "anna", null),
            new UserBriefResponse(6L, "hanna", null)
        ));

        assertThat(userBriefs.searchUserIds("ann")).containsExactly(5L, 6L);
    }

    @Test
    void unavailableIdentityIsReportedAsServiceUnavailable() {
        when(identityMsService.searchUserBriefs("ann")).thenThrow(new UpstreamHttpException("unavailable", 503, null));

        assertThatThrownBy(() -> userBriefs.searchUserIds("ann"))
            .isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void openCircuitIsReportedAsServiceUnavailable() {
        when(identityMsService.searchUserBriefs("ann"))
            .thenThrow(CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("identity-ms")));

        assertThatThrownBy(() -> userBriefs.searchUserIds("ann"))
            .isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
