package com.rentalert.processing_ms.repository.projection;

import com.rentalert.dto.listing.ScheduleProvider;

public interface DailyProviderArrivalProjection {

    int getArrivalYear();

    int getArrivalMonth();

    int getArrivalDay();

    ScheduleProvider getProvider();

    long getTotal();
}
