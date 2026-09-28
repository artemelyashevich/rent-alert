package com.rentalert.internal.resilience;

import com.rentalert.common.exception.UpstreamHttpException;
import feign.Request;
import feign.RetryableException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class ResiliencePredicatesTests {

    private final RetryableExceptionPredicate retryPredicate = new RetryableExceptionPredicate();
    private final CircuitBreakerFailurePredicate failurePredicate = new CircuitBreakerFailurePredicate();

    @Test
    void retriesFeignNetworkErrors() {
        assertThat(retryPredicate.test(networkError())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 502, 503, 504})
    void retriesTransientUpstreamStatuses(int status) {
        assertThat(retryPredicate.test(upstream(status))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 500})
    void doesNotRetryPermanentUpstreamStatuses(int status) {
        assertThat(retryPredicate.test(upstream(status))).isFalse();
    }

    @Test
    void doesNotRetryOtherExceptions() {
        assertThat(retryPredicate.test(new IllegalStateException())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 503})
    void circuitBreakerRecordsUpstreamOutages(int status) {
        assertThat(failurePredicate.test(upstream(status))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 404, 409})
    void circuitBreakerIgnoresBusinessErrors(int status) {
        assertThat(failurePredicate.test(upstream(status))).isFalse();
    }

    @Test
    void circuitBreakerRecordsNetworkErrors() {
        assertThat(failurePredicate.test(networkError())).isTrue();
    }

    private static UpstreamHttpException upstream(int status) {
        return new UpstreamHttpException("upstream", status, null);
    }

    private static RetryableException networkError() {
        Request request = Request.create(Request.HttpMethod.GET, "http://identity-ms/x", Map.of(), null, UTF_8, null);
        return new RetryableException(-1, "Connection refused", Request.HttpMethod.GET, (Long) null, request);
    }
}
