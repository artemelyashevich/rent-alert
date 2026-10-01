package com.rentalert.identity_ms.controller;

import com.rentalert.dto.auth.EmailActivationRequest;
import com.rentalert.dto.auth.OtpConfirmationRequest;
import com.rentalert.dto.auth.OtpPreparationResponse;
import com.rentalert.identity_ms.config.PublicUrls;
import com.rentalert.identity_ms.service.EmailActivationService;
import com.rentalert.redis.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email", description = "Привязка и подтверждение почты")
@RestController
@RequestMapping(PublicUrls.EMAIL_TAG)
@RequiredArgsConstructor
public class EmailController {

    private final EmailActivationService emailActivationService;
    private final UserSessionService userSessionService;

    @Operation(summary = "Указать почту и отправить на неё код подтверждения")
    @PostMapping("/preparation")
    public OtpPreparationResponse prepare(@Valid @RequestBody EmailActivationRequest request) {
        return emailActivationService.prepare(userSessionService.getCurrentUser().getUserId(), request.email());
    }

    @Operation(summary = "Подтвердить почту кодом из письма")
    @PostMapping("/activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@Valid @RequestBody OtpConfirmationRequest request) {
        emailActivationService.activate(userSessionService.getCurrentUser().getUserId(), request);
    }
}
