package com.rentalert.internal.client;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.internal.resilience.CircuitBreakerFailurePredicate;
import com.rentalert.internal.resilience.RetryableExceptionPredicate;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractResilientClientTests {

    private final RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1))
            .retryOnException(new RetryableExceptionPredicate())
            .build());

    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .failureRateThreshold(50)
            .recordException(new CircuitBreakerFailurePredicate())
            .build());

    static class TestClient extends AbstractResilientClient {
        TestClient(RetryRegistry retry, CircuitBreakerRegistry circuitBreaker) {
            super("test", retry, circuitBreaker);
        }

        <T> T call(Supplier<T> supplier) {
            return execute(supplier);
        }
    }

    @Test
    void retriesTransientFailureAndReturnsResult() {
        TestClient client = new TestClient(retryRegistry, CircuitBreakerRegistry.ofDefaults());
        AtomicInteger attempts = new AtomicInteger();

        String result = client.call(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new UpstreamHttpException("unavailable", 503, null);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void doesNotRetryBusinessError() {
        TestClient client = new TestClient(retryRegistry, CircuitBreakerRegistry.ofDefaults());
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> client.call(() -> {
            attempts.incrementAndGet();
            throw new UpstreamHttpException("not found", 404, null);
        })).isInstanceOf(UpstreamHttpException.class);

        assertThat(attempts).hasValue(1);
    }

    @Test
    void opensCircuitAndStopsCallingUpstream() {
        TestClient client = new TestClient(retryRegistry, circuitBreakerRegistry);
        AtomicInteger attempts = new AtomicInteger();
        Supplier<String> failing = () -> {
            attempts.incrementAndGet();
            throw new UpstreamHttpException("unavailable", 503, null);
        };

        // Две попытки открывают цепь, третья отбивается circuit breaker'ом и не ретраится
        assertThatThrownBy(() -> client.call(failing)).isInstanceOf(CallNotPermittedException.class);
        assertThat(attempts).hasValue(2);

        assertThatThrownBy(() -> client.call(failing)).isInstanceOf(CallNotPermittedException.class);
        assertThat(attempts).hasValue(2);
    }
}
