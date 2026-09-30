package com.rentalert.identity_ms.controller;

import com.rentalert.dto.profile.FeedbackRequest;
import com.rentalert.dto.profile.FeedbackResponse;
import com.rentalert.identity_ms.config.PublicUrls;
import com.rentalert.identity_ms.service.FeedbackService;
import com.rentalert.redis.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Feedback", description = "Управление отзывами пользователей")
@RestController
@RequestMapping(PublicUrls.FEEDBACK_TAG)
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserSessionService userSessionService;

    @Operation(
        summary = "Создать отзыв",
        description = "Позволяет пользователю оставить отзыв с оценкой от 1 до 5 звёзд"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Отзыв успешно создан",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FeedbackResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные запроса",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не авторизован",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse createFeedback(@Valid @RequestBody FeedbackRequest request) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return feedbackService.createFeedback(request, userId);
    }
}
