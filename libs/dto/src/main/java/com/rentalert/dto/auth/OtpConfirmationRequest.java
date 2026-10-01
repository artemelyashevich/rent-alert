package com.rentalert.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record OtpConfirmationRequest(
    @NotBlank String requestId,
    @NotBlank String code
) {

}
