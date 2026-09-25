package com.rentalert.common.exception;

import org.springframework.http.HttpStatus;

public class AppRateLimitException extends AppException {

    private final Long retryAfterSeconds;

    public AppRateLimitException(String message, Long retryAfterSeconds) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}