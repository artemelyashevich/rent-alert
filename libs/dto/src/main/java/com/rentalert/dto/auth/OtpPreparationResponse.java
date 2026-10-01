package com.rentalert.dto.auth;

import java.time.Instant;

public record OtpPreparationResponse(String requestId, Instant expiresAt) {

}
