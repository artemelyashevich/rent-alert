package com.rentalert.listing_ms.controller.internal;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.listing.FavoriteAddResult;
import com.rentalert.dto.listing.FavoriteViewingDto;
import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.listing_ms.domain.ListingStatusName;
import com.rentalert.listing_ms.dto.FavoriteCreateDto;
import com.rentalert.listing_ms.service.FavoriteService;
import com.rentalert.listing_ms.service.ListingStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_FAVORITE_TAG)
@RequiredArgsConstructor
public class FavoriteInternalController {

    private final FavoriteService favoriteService;
    private final ListingStatusService listingStatusService;

    @GetMapping("/{userId}/{listingId}")
    public ResponseEntity<FavoriteViewingDto> findViewing(@PathVariable Long userId, @PathVariable Long listingId) {
        return favoriteService.findViewing(userId, listingId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{userId}/{listingId}")
    public FavoriteAddResult add(@PathVariable Long userId, @PathVariable Long listingId) {
        Long statusId = listingStatusService.findByName(ListingStatusName.INTERESTED).getId();
        try {
            favoriteService.addToFavorites(userId, new FavoriteCreateDto(listingId, statusId, null));
            return FavoriteAddResult.ADDED;
        } catch (AppException e) {
            if (e.getErrorCode() == InternalExceptionCode.EX400_FAVORITE_ALREADY_EXISTS) {
                return FavoriteAddResult.ALREADY_EXISTS;
            }
            if (e.getErrorCode() == InternalExceptionCode.EX400_FAVORITE_LIMIT_REACHED) {
                return FavoriteAddResult.LIMIT_REACHED;
            }
            throw e;
        }
    }

    @DeleteMapping("/{userId}/{listingId}")
    public boolean remove(@PathVariable Long userId, @PathVariable Long listingId) {
        try {
            favoriteService.removeFromFavorites(userId, listingId);
            return true;
        } catch (AppException e) {
            if (e.getErrorCode() == InternalExceptionCode.EX404_FAVORITE_NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }
}
