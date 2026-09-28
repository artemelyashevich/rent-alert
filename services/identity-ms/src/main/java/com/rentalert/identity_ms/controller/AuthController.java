package com.rentalert.identity_ms.controller;

import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.auth.TelegramAuthRequest;
import com.rentalert.identity_ms.config.PublicUrls;
import com.rentalert.identity_ms.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Аутентификация пользователей через Telegram")
@RestController
@RequestMapping(PublicUrls.AUTH_TAG)
@RequiredArgsConstructor
public class AuthController {

    private final LoginService<TelegramAuthRequest> telegramUserAuthService;

    @Operation(
            summary = "Вход через Telegram",
            description = "Аутентифицирует пользователя по данным Telegram и возвращает JWT-токен"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная аутентификация",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса (ошибка валидации)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверная подпись Telegram или данные устарели",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PostMapping("/login")
    public ResponseEntity<@NonNull LoginResponse> login(@Valid @RequestBody TelegramAuthRequest request) {
        return ResponseEntity.ok(telegramUserAuthService.auth(request));
    }
}