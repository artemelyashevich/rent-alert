package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.client.IdentityAdminClient;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.dto.auth.AdminLoginRequest;
import com.rentalert.dto.auth.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin auth", description = "Вход администратора")
@RestController
@RequestMapping(PublicUrls.AUTH_TAG)
@RequiredArgsConstructor
public class AdminAuthController {

    private final IdentityAdminClient identityAdminClient;

    @Operation(summary = "Вход администратора по логину и паролю")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return identityAdminClient.login(request);
    }
}
