package com.rentalert.processing_ms.repository.projection;

import com.rentalert.dto.listing.ScheduleProvider;

public interface ProviderListingCountProjection {

    ScheduleProvider getProvider();

    long getTotal();

    long getActive();
}
