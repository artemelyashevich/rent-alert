package com.rentalert.identity_ms.controller;

import com.rentalert.identity_ms.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles/email")
@RequiredArgsConstructor
public class EmailController {

    private final UserProfileService userProfileService;

    @PostMapping("/preparation")
    public OtpPreparationResponse activatePreparation(@RequestBody UserEmailActivationPreparationRequest emailActivationRequest) {
        return userProfileService.emailActivatePreparation(emailActivationRequest);
    }

    @PostMapping("/activation")
    public boolean activation(@RequestBody OtpConfirmationRequest otpConfirmationRequest) {
        return userProfileService.activateEmail(otpConfirmationRequest);
    }
}
