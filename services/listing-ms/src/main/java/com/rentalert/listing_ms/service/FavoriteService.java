package com.rentalert.listing_ms.service;

import com.rentalert.dto.listing.FavoriteViewingDto;
import com.rentalert.listing_ms.domain.ListingStatusName;
import com.rentalert.listing_ms.dto.FavoriteCreateDto;
import com.rentalert.listing_ms.dto.FavoriteResponse;
import com.rentalert.listing_ms.dto.FavoriteUpdateStatusDto;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteService {

    FavoriteResponse addToFavorites(Long userId, FavoriteCreateDto createDto);

    Page<FavoriteResponse> getUserFavorites(Long userId, ListingStatusName status, Pageable pageable);

    FavoriteResponse updateStatus(Long userId, Long listingId, FavoriteUpdateStatusDto updateDto);

    void removeFromFavorites(Long userId, Long listingId);

    Optional<FavoriteViewingDto> findViewing(Long userId, Long listingId);
}
