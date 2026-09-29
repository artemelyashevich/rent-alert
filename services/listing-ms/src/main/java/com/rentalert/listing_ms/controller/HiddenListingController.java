package com.rentalert.listing_ms.controller;

import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.listing_ms.service.HiddenListingService;
import com.rentalert.redis.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hidden listings", description = "Объявления, скрытые пользователем")
@RestController
@RequestMapping(PublicUrls.HIDDEN_LISTING_TAG)
@RequiredArgsConstructor
public class HiddenListingController {

    private final HiddenListingService hiddenListingService;
    private final UserSessionService userSessionService;

    @Operation(
            summary = "Скрыть объявление",
            description = "Скрывает объявление для текущего пользователя (аналог кнопки \"Скрыть\" в боте)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Объявление успешно скрыто"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Объявление уже скрыто",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Объявление не найдено",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PostMapping("/{listingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void hideListing(
            @Parameter(description = "ID объявления") @PathVariable Long listingId
    ) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        hiddenListingService.hideListing(userId, listingId);
    }

    @Operation(
            summary = "Получить скрытые объявления",
            description = "Возвращает страницу объявлений, которые текущий пользователь скрыл (например, кнопкой \"Скрыть\" в боте)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Страница со скрытыми объявлениями успешно получена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ListingDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping
    public Page<ListingDto> getHiddenListings(
            @Parameter(description = "Параметры пагинации и сортировки (page, size, sort)")
            @ParameterObject Pageable pageable
    ) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return hiddenListingService.getUserHiddenListings(userId, pageable);
    }

    @Operation(
            summary = "Удалить объявление из скрытых",
            description = "Удаляет объявление из списка скрытых пользователя."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Объявление успешно удалено из скрытых"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Скрытое объявление не найдено",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromHidden(
            @Parameter(description = "ID объявления") @PathVariable Long listingId
    ) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        hiddenListingService.removeFromHidden(userId, listingId);
    }
}
