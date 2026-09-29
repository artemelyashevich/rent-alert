package com.rentalert.internal.client;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.internal.client.service.IdentityMsServiceImpl;
import com.rentalert.internal.resilience.RetryableExceptionPredicate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityMsServiceImplTests {

    private final IdentityMsClient client = mock(IdentityMsClient.class);
    private final IdentityMsService service = new IdentityMsServiceImpl(
        client,
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
    void doesNotRetryNotFound() {
        when(client.getProfile(42L)).thenThrow(new UpstreamHttpException("not found", 404, null));

        assertThatThrownBy(() -> service.getProfile(42L)).isInstanceOf(UpstreamHttpException.class);
        verify(client).getProfile(42L);
    }
}
