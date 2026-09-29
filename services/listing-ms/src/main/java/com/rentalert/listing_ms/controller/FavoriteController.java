package com.rentalert.listing_ms.controller;

import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.listing_ms.dto.FavoriteCreateDto;
import com.rentalert.listing_ms.dto.FavoriteResponse;
import com.rentalert.listing_ms.dto.FavoriteUpdateStatusDto;
import com.rentalert.listing_ms.domain.ListingStatusName;
import com.rentalert.listing_ms.service.FavoriteService;
import com.rentalert.redis.service.UserSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Favorites", description = "Управление избранными объявлениями пользователя")
@RestController
@RequestMapping(PublicUrls.FAVORITE_TAG)
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserSessionService userSessionService;

    @Operation(
            summary = "Получить избранные объявления",
            description = "Возвращает список всех избранных объявлений текущего пользователя с поддержкой пагинации. " +
                    "Максимальное количество избранных: 30. " +
                    "Доступные поля для сортировки: createdAt, updatedAt, viewingDate"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список избранных успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FavoriteResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping
    public Page<FavoriteResponse> getUserFavorites(
            @Parameter(description = "Фильтр по статусу объявления (например, INTERESTED, VIEWED)")
            @RequestParam(required = false) ListingStatusName status,

            @Parameter(description = "Параметры пагинации и сортировки (page, size, sort)")
            @ParameterObject
            @PageableDefault(size = 30) Pageable pageable
    ) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return favoriteService.getUserFavorites(userId, status, pageable);
    }

    @Operation(
            summary = "Добавить объявление в избранное",
            description = "Добавляет объявление в избранное с указанным статусом. " +
                    "Ограничение: не более 30 избранных объявлений на пользователя."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Объявление успешно добавлено в избранное",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FavoriteResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Объявление уже в избранном или достигнут лимит (30 объявлений)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Объявление или статус не найдены",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteResponse addToFavorites(@Valid @RequestBody FavoriteCreateDto createDto) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return favoriteService.addToFavorites(userId, createDto);
    }

    @Operation(
            summary = "Обновить статус избранного объявления",
            description = "Изменяет статус избранного объявления и дату просмотра (опционально). " +
                    "Доступные статусы: INTERESTED, WAITING_RESPONSE, VIEWING_SCHEDULED, RESERVED, VIEWED, AGREED."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус успешно обновлён",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FavoriteResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Избранное объявление или статус не найдены",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PutMapping("/{listingId}")
    public FavoriteResponse updateStatus(
            @Parameter(description = "ID объявления") @PathVariable Long listingId,
            @Valid @RequestBody FavoriteUpdateStatusDto updateDto
    ) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        return favoriteService.updateStatus(userId, listingId, updateDto);
    }

    @Operation(
            summary = "Удалить объявление из избранного",
            description = "Удаляет объявление из списка избранных пользователя."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Объявление успешно удалено из избранного"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Избранное объявление не найдено",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromFavorites(
            @Parameter(description = "ID объявления") @PathVariable Long listingId
    ) {
        Long userId = userSessionService.getCurrentUser().getUserId();
        favoriteService.removeFromFavorites(userId, listingId);
    }
}
