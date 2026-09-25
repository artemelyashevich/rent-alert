package com.rentalert.identity_ms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "User Filters", description = "Управление фильтрами пользователя для получения уведомлений об объявлениях")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/filters")
@RequiredArgsConstructor
public class UserFilterController {

    private final UserFilterService userFilterService;

    @Operation(
            summary = "Получить все фильтры профиля",
            description = "Возвращает список всех фильтров для указанного профиля пользователя"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список фильтров успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = UserFilterResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Профиль пользователя не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping
    public ResponseEntity<@NonNull List<UserFilterResponse>> findAll() {
        return ResponseEntity.ok(userFilterService.findAll());
    }

    @Operation(
            summary = "Получить фильтр по идентификатору",
            description = "Возвращает фильтр пользователя по его идентификатору"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Фильтр успешно найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserFilterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Фильтр или профиль не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/{filterId}")
    public ResponseEntity<@NonNull UserFilterResponse> findById(
            @Parameter(description = "Идентификатор фильтра", example = "3", required = true)
            @PathVariable Long filterId
    ) {
        return ResponseEntity.ok(userFilterService.findById(filterId));
    }

    @Operation(
            summary = "Создать фильтр",
            description = "Создаёт новый фильтр для профиля пользователя. " +
                    "Максимальное количество фильтров на профиль — 5. " +
                    "Возвращает URI созданного ресурса в заголовке Location."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Фильтр успешно создан",
                    headers = @Header(
                            name = "Location",
                            description = "URI созданного ресурса",
                            schema = @Schema(type = "string", example = "/api/v1/user-profiles/1/filters/3")
                    ),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserFilterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса или превышен лимит фильтров",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Профиль пользователя не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping
    public ResponseEntity<@NonNull UserFilterResponse> create(
            @Valid @RequestBody UserFilterRequest request,
            UriComponentsBuilder uriComponentsBuilder
    ) {
        UserFilterResponse response = userFilterService.create(request);
        return ResponseEntity.created(
                uriComponentsBuilder
                        .replacePath("/api/v1/user-profiles/filters/{filterId}")
                        .build(Map.of("filterId", response.getId()))
        ).body(response);
    }

    @Operation(
            summary = "Обновить фильтр",
            description = "Обновляет параметры существующего фильтра пользователя"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Фильтр успешно обновлён",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserFilterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Фильтр или профиль не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PutMapping("/{filterId}")
    public ResponseEntity<@NonNull UserFilterResponse> update(
            @Parameter(description = "Идентификатор фильтра", example = "3", required = true)
            @PathVariable Long filterId,
            @Valid @RequestBody UserFilterRequest request
    ) {
        return ResponseEntity.ok(userFilterService.update(filterId, request));
    }

    @Operation(
            summary = "Установить состояние активации фильтра",
            description = "Устанавливает состояние активации фильтра. При активации (active=true) ранее активный фильтр профиля деактивируется автоматически"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Состояние активации фильтра успешно установлено",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserFilterResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Фильтр или профиль не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PatchMapping("/{filterId}/activation/{active}")
    public ResponseEntity<@NonNull UserFilterResponse> setActivation(
            @Parameter(description = "Идентификатор фильтра", example = "3", required = true)
            @PathVariable Long filterId,
            @Parameter(description = "Состояние активации", example = "true", required = true)
            @PathVariable Boolean active
    ) {
        return ResponseEntity.ok(userFilterService.setActivation(filterId, active));
    }

    @Operation(
            summary = "Удалить фильтр",
            description = "Удаляет фильтр пользователя по его идентификатору. Активный фильтр не может быть удалён - его необходимо сначала деактивировать"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Фильтр успешно удалён"),
            @ApiResponse(responseCode = "400", description = "Попытка удалить активный фильтр",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Фильтр или профиль не найден",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @DeleteMapping("/{filterId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Идентификатор фильтра", example = "3", required = true)
            @PathVariable Long filterId
    ) {
        userFilterService.delete(filterId);
        return ResponseEntity.noContent().build();
    }
}