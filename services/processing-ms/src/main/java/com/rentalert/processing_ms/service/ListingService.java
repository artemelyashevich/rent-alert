package com.rentalert.processing_ms.service;

import com.rentalert.processing_ms.domain.Listing;

public interface ListingService {

    /**
     * Saves a newly parsed listing and publishes it to listing-ms in the same transaction. Throws {@link org.springframework.dao.DataIntegrityViolationException} if the listing already exists.
     */
    Listing saveNew(Listing listing);
}
