package com.rentalert.dto.auth;

import java.time.Instant;

public record EmailOtp(Long userId, String email, String code, int failedAttempts, Instant expiresAt) {

    public EmailOtp withFailedAttempt() {
        return new EmailOtp(userId, email, code, failedAttempts + 1, expiresAt);
    }
}
