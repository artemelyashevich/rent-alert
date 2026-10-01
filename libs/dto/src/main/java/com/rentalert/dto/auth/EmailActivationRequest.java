package com.rentalert.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailActivationRequest(
    @NotBlank @Email @Size(max = 255) String email
) {

}
