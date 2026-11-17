package com.rentalert.roommate_ms.controller;

import com.rentalert.redis.service.UserSessionService;
import com.rentalert.roommate_ms.config.PublicUrls;
import com.rentalert.roommate_ms.dto.RoommateListingRequest;
import com.rentalert.roommate_ms.dto.RoommateListingResponse;
import com.rentalert.roommate_ms.dto.RoommateListingStatusRequest;
import com.rentalert.roommate_ms.service.RoommateListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Roommate listings", description = "Объявления о подселении (сценарий A)")
@RestController
@RequestMapping(PublicUrls.LISTING_TAG)
@RequiredArgsConstructor
public class RoommateListingController {

    private final RoommateListingService listingService;
    private final UserSessionService userSessionService;

    @Operation(summary = "Создать объявление о подселении")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoommateListingResponse create(@Valid @RequestBody RoommateListingRequest request) {
        return listingService.create(currentUserId(), request);
    }

    @Operation(summary = "Мои объявления о подселении")
    @GetMapping("/mine")
    public Page<RoommateListingResponse> findMine(
        @Parameter(description = "Параметры пагинации и сортировки")
        @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return listingService.findMine(currentUserId(), pageable);
    }

    @Operation(summary = "Одно моё объявление")
    @GetMapping("/{listingId}")
    public RoommateListingResponse getOne(@PathVariable Long listingId) {
        return listingService.getOwned(listingId, currentUserId());
    }

    @Operation(summary = "Обновить объявление")
    @PutMapping("/{listingId}")
    public RoommateListingResponse update(@PathVariable Long listingId, @Valid @RequestBody RoommateListingRequest request) {
        return listingService.update(listingId, currentUserId(), request);
    }

    @Operation(
        summary = "Сменить статус объявления",
        description = "ACTIVE | PAUSED | ARCHIVED. Не более одного ACTIVE-объявления на автора одновременно."
    )
    @PatchMapping("/{listingId}/status")
    public RoommateListingResponse setStatus(@PathVariable Long listingId, @Valid @RequestBody RoommateListingStatusRequest request) {
        return listingService.setStatus(listingId, currentUserId(), request.status());
    }

    private Long currentUserId() {
        return userSessionService.getCurrentUser().getUserId();
    }
}
