package com.rentalert.internal.client.service;

import com.rentalert.dto.listing.FavoriteAddResult;
import com.rentalert.dto.listing.FavoriteViewingDto;
import com.rentalert.dto.listing.ListingDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ListingMsService {

    Optional<FavoriteViewingDto> getFavoriteViewing(Long userId, Long listingId);

    FavoriteAddResult addFavorite(Long userId, Long listingId);

    boolean removeFavorite(Long userId, Long listingId);

    boolean hideListing(Long userId, Long listingId);

    /**
     * Listing cards by id. Ids of deleted listings are absent from the result; an empty input makes no call.
     */
    List<ListingDto> findCards(Collection<Long> ids);
}
