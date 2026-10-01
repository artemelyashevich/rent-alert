package com.rentalert.processing_ms.repository.projection;

import com.rentalert.dto.listing.ScheduleProvider;

public interface CityProviderCountProjection {

    String getCity();

    ScheduleProvider getProvider();

    long getTotal();
}
