package com.rentalert.internal.client.service;

import com.rentalert.dto.listing.FavoriteViewingDto;
import java.util.Optional;

public interface ListingMsService {

    Optional<FavoriteViewingDto> getFavoriteViewing(Long userId, Long listingId);
}
