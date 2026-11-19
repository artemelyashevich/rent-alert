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

    protected AbstractResilientClient(String instanceName, RetryRegistry retryRegistry) {
        this.retry = retryRegistry.retry(instanceName);
        this.circuitBreaker = null;
    }

    protected <T> T execute(Supplier<T> call) {
        Supplier<T> guarded = circuitBreaker == null ? call : CircuitBreaker.decorateSupplier(circuitBreaker, call);
        return Retry.decorateSupplier(retry, guarded).get();
    }

    protected void execute(Runnable call) {
        Runnable guarded = circuitBreaker == null ? call : CircuitBreaker.decorateRunnable(circuitBreaker, call);
        Retry.decorateRunnable(retry, guarded).run();
    }

    protected <T> T executeOnce(Supplier<T> call) {
        return circuitBreaker == null ? call.get() : CircuitBreaker.decorateSupplier(circuitBreaker, call).get();
    }

    protected void executeOnce(Runnable call) {
        if (circuitBreaker == null) {
            call.run();
            return;
        }
        CircuitBreaker.decorateRunnable(circuitBreaker, call).run();
    }
}
