package com.rentalert.listing_ms.controller.internal;

import com.rentalert.dto.listing.FavoriteViewingDto;
import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.listing_ms.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_FAVORITE_TAG)
@RequiredArgsConstructor
public class FavoriteInternalController {

    private final FavoriteService favoriteService;

    @GetMapping("/{userId}/{listingId}")
    public ResponseEntity<FavoriteViewingDto> findViewing(@PathVariable Long userId, @PathVariable Long listingId) {
        return favoriteService.findViewing(userId, listingId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
