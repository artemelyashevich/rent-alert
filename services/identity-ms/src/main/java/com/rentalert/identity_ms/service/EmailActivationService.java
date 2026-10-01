package com.rentalert.identity_ms.service;

import com.rentalert.dto.auth.OtpConfirmationRequest;
import com.rentalert.dto.auth.OtpPreparationResponse;

public interface EmailActivationService {

    OtpPreparationResponse prepare(Long userId, String email);

    void activate(Long userId, OtpConfirmationRequest request);
}
