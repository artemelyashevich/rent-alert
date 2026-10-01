package com.rentalert.processing_ms.repository.projection;

import com.rentalert.dto.listing.ScheduleProvider;

public interface ProviderArrivalProjection {

    ScheduleProvider getProvider();

    long getTotal();
}
