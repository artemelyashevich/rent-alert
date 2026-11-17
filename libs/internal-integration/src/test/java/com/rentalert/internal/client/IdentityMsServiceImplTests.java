package com.rentalert.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.notification.NotificationDelivery;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.internal.client.service.impl.IdentityMsServiceImpl;
import com.rentalert.internal.resilience.RetryableExceptionPredicate;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdentityMsServiceImplTests {

    private final IdentityMsClient client = mock(IdentityMsClient.class);
    private final CacheService cacheService = mock(CacheService.class);
    private final IdentityMsService service = new IdentityMsServiceImpl(
        client,
        cacheService,
        RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1))
            .retryOnException(new RetryableExceptionPredicate())
            .build()),
        CircuitBreakerRegistry.ofDefaults()
    );

    @Test
    void retriesUnavailableIdentity() {
        UserProfileResponse profile = mock(UserProfileResponse.class);
        when(client.getProfile(42L))
            .thenThrow(new UpstreamHttpException("unavailable", 503, null))
            .thenReturn(profile);

        assertThat(service.getProfile(42L)).isSameAs(profile);
        verify(client, times(2)).getProfile(42L);
    }

    @Test
    void fetchesReceiverWithoutCache() {
        NotificationRequest.Receiver receiver = new NotificationRequest.Receiver("tg-enc", null);
        when(client.getReceiver(42L)).thenReturn(receiver);

        assertThat(service.getReceiver(42L)).isEqualTo(receiver);
        verify(cacheService, never()).put(any(), any(), any(), any());
    }

    @Test
    void retriesDeliveryLookup() {
        NotificationDelivery delivery = new NotificationDelivery(new NotificationRequest.Receiver("tg-enc", null),
            Instant.parse("2026-10-02T05:00:00Z"));
        when(client.getDelivery(42L))
            .thenThrow(new UpstreamHttpException("unavailable", 503, null))
            .thenReturn(delivery);

        assertThat(service.getDelivery(42L)).isEqualTo(delivery);
        verify(client, times(2)).getDelivery(42L);
    }

    @Test
    void userBriefsAreFetchedWithoutAdminPath() {
        List<UserBriefResponse> briefs = List.of(new UserBriefResponse(42L, "anna", null));
        when(client.findUserBriefs(List.of(42L))).thenReturn(briefs);

        assertThat(service.findUserBriefs(List.of(42L))).isEqualTo(briefs);
        verify(client, never()).findBriefs(any());
    }

    @Test
    void emptyUserBriefsRequestMakesNoCall() {
        assertThat(service.findUserBriefs(List.of())).isEmpty();
        verify(client, never()).findUserBriefs(any());
    }

    @Test
    void unknownTelegramUserIsEmpty() {
        when(client.findByTelegramId(any())).thenThrow(new UpstreamHttpException("not found", 404, null));

        assertThat(service.findByTelegramId("100")).isEmpty();
    }

    @Test
    void doesNotRetryNotFound() {
        when(client.getProfile(42L)).thenThrow(new UpstreamHttpException("not found", 404, null));

        assertThatThrownBy(() -> service.getProfile(42L)).isInstanceOf(UpstreamHttpException.class);
        verify(client).getProfile(42L);
    }

    @Test
    void returnsCachedProfileWithoutCallingIdentity() {
        UserProfileResponse profile = mock(UserProfileResponse.class);
        when(cacheService.get(CacheType.USER_PROFILE_INTERNAL, 42L)).thenReturn(Optional.of(profile));

        assertThat(service.getProfile(42L)).isSameAs(profile);
        verify(client, never()).getProfile(any());
    }

    @Test
    void cachesLoadedActiveFilter() {
        UserFilterResponse filter = UserFilterResponse.builder().id(3L).active(true).build();
        when(client.getActiveFilter(42L)).thenReturn(filter);

        assertThat(service.getActiveFilter(42L)).containsSame(filter);
        verify(cacheService).put(eq(CacheType.USER_ACTIVE_FILTER), eq(42L), eq(filter), any());
    }

    @Test
    void absentActiveFilterIsNotCached() {
        assertThat(service.getActiveFilter(42L)).isEmpty();
        verify(cacheService, never()).put(eq(CacheType.USER_ACTIVE_FILTER), any(), any(), any());
    }

    @Test
    void absentMatchFilterIsEmpty() {
        when(client.getMatchFilter(7L)).thenReturn(null);

        assertThat(service.getMatchFilter(7L)).isEmpty();
    }

    @Test
    void upsertsMatchFilterWithoutRetry() {
        UserFilterRequest request = UserFilterRequest.builder().city("Минск").build();
        UserFilterResponse filter = UserFilterResponse.builder().id(3L).active(false).build();
        when(client.upsertMatchFilter(7L, request)).thenReturn(filter);

        assertThat(service.upsertMatchFilter(7L, request)).isSameAs(filter);
        verify(client).upsertMatchFilter(7L, request);
    }

    @Test
    void matchFilterActivationIsNotRetried() {
        when(client.setMatchFilterActivation(7L, true))
            .thenThrow(new UpstreamHttpException("unavailable", 503, null));

        assertThatThrownBy(() -> service.setMatchFilterActivation(7L, true))
            .isInstanceOf(UpstreamHttpException.class);
        verify(client).setMatchFilterActivation(7L, true);
    }
}
