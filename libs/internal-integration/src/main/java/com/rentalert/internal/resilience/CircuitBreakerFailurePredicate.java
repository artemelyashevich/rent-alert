package com.rentalert.internal.resilience;

import com.rentalert.common.exception.UpstreamHttpException;
import java.util.function.Predicate;

public class CircuitBreakerFailurePredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof UpstreamHttpException upstream) {
            return upstream.isServerError() || upstream.isRateLimited();
        }
        return true;
    }
}
