package com.rentalert.internal.resilience;

import com.rentalert.common.exception.UpstreamHttpException;
import feign.RetryableException;
import java.util.Set;
import java.util.function.Predicate;

public class RetryableExceptionPredicate implements Predicate<Throwable> {

    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 502, 503, 504);

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof RetryableException) {
            return true;
        }
        return throwable instanceof UpstreamHttpException upstream
            && RETRYABLE_STATUSES.contains(upstream.getUpstreamStatus());
    }
}
