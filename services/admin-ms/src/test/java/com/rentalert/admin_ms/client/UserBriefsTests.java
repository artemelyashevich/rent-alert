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
import java.util.stream.LongStream;
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
    void usernameSearchIsCappedAtThousandUserIds() {
        List<UserBriefResponse> briefs = LongStream.rangeClosed(1, 1500)
            .mapToObj(id -> new UserBriefResponse(id, "user" + id, null))
            .toList();
        when(identityMsService.searchUserBriefs("user")).thenReturn(briefs);

        List<Long> userIds = userBriefs.searchUserIds("user");

        assertThat(userIds).hasSize(1000).first().isEqualTo(1L);
        assertThat(userIds).last().isEqualTo(1000L);
    }

    @Test
    void ifAvailableSearchReturnsMatchesWhenIdentityResponds() {
        when(identityMsService.searchUserBriefs("ann")).thenReturn(List.of(new UserBriefResponse(5L, "anna", null)));

        assertThat(userBriefs.searchUserIdsIfAvailable("ann")).containsExactly(5L);
    }

    @Test
    void ifAvailableSearchReturnsEmptyWhenIdentityIsUnavailable() {
        when(identityMsService.searchUserBriefs("ann")).thenThrow(new UpstreamHttpException("unavailable", 503, null));

        assertThat(userBriefs.searchUserIdsIfAvailable("ann")).isEmpty();
    }

    @Test
    void ifAvailableSearchReturnsEmptyWhenCircuitIsOpen() {
        when(identityMsService.searchUserBriefs("ann"))
            .thenThrow(CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("identity-ms")));

        assertThat(userBriefs.searchUserIdsIfAvailable("ann")).isEmpty();
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
