package com.rentalert.internal.client.service;

import com.rentalert.dto.listing.FavoriteAddResult;
import com.rentalert.dto.listing.FavoriteViewingDto;
import java.util.Optional;

public interface ListingMsService {

    Optional<FavoriteViewingDto> getFavoriteViewing(Long userId, Long listingId);

    FavoriteAddResult addFavorite(Long userId, Long listingId);

    boolean removeFavorite(Long userId, Long listingId);

    boolean hideListing(Long userId, Long listingId);
}
