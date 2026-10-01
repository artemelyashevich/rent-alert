package com.rentalert.dto.listing;

import java.time.Instant;

public record FavoriteViewingDto(Long userId, Long listingId, Instant viewingDate, ListingDto listing) {

}
