package com.rentalert.listing_ms.controller;

import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.listing_ms.service.ListingService;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Set;

@Tag(name = "Listings", description = "Объявления об аренде квартир, собранные с парсингуемых сайтов")
@RestController
@RequestMapping(PublicUrls.LISTING_TAG)
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @Operation(
            summary = "Получить список объявлений",
            description = "Возвращает страницу объявлений, соответствующих активному фильтру пользователя. " +
                    "Если у пользователя нет активных фильтров, возвращается ошибка 404."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Страница с объявлениями успешно получена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ListingDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные параметры пагинации/сортировки",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не авторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "У пользователя нет активных фильтров",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping
    public Page<ListingDto> findAll(
            @Parameter(description = "Список провайдеров для фильтрации (kufar, onliner, realt в любом регистре)")
            @RequestParam(required = false) Set<String> sources,
            @Parameter(description = "Категория объявления (FLAT — квартиры, ROOM — комнаты). Не задано — обе категории")
            @RequestParam(required = false) ListingCategory category,
            @Parameter(description = "Параметры пагинации и сортировки (page, size, sort)")
            @ParameterObject Pageable pageable,
        @Parameter(description = "Параметр для поиска по ключевым атрибутам")
            @RequestParam(required = false, name = "filter") String filter
    ) {
        return listingService.findAll(filter, sources, category, pageable);
    }

    @Operation(
            summary = "Получить список квартир",
            description = "То же самое, что GET /core/listings?category=FLAT."
    )
    @GetMapping("/flats")
    public Page<ListingDto> findAllFlats(
            @RequestParam(required = false) Set<String> sources,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false, name = "filter") String filter
    ) {
        return listingService.findAll(filter, sources, ListingCategory.FLAT, pageable);
    }

    @Operation(
            summary = "Получить список комнат",
            description = "То же самое, что GET /core/listings?category=ROOM."
    )
    @GetMapping("/rooms")
    public Page<ListingDto> findAllRooms(
            @RequestParam(required = false) Set<String> sources,
            @ParameterObject Pageable pageable,
            @RequestParam(required = false, name = "filter") String filter
    ) {
        return listingService.findAll(filter, sources, ListingCategory.ROOM, pageable);
    }
}
