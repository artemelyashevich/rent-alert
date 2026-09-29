package com.rentalert.listing_ms.service;

import com.rentalert.dto.listing.ListingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HiddenListingService {

    void hideListing(Long userId, Long listingId);

    Page<ListingDto> getUserHiddenListings(Long userId, Pageable pageable);

    void removeFromHidden(Long userId, Long listingId);
}
