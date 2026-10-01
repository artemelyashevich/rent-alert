package com.rentalert.processing_ms.repository.projection;

import com.rentalert.dto.listing.ListingCategory;

public interface CategoryListingCountProjection {

    ListingCategory getCategory();

    long getTotal();

    long getActive();
}
