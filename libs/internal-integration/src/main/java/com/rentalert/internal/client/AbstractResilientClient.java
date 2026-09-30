package com.rentalert.internal.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.function.Supplier;

public abstract class AbstractResilientClient {

    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    protected AbstractResilientClient(String instanceName, RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        this.retry = retryRegistry.retry(instanceName);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName);
    }

    protected <T> T execute(Supplier<T> call) {
        return Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, call)).get();
    }

    protected void execute(Runnable call) {
        Retry.decorateRunnable(retry, CircuitBreaker.decorateRunnable(circuitBreaker, call)).run();
    }
}
