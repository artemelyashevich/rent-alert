package com.rentalert.processing_ms.service;

import com.rentalert.dto.statistics.ListingStatisticsOverview;
import java.time.LocalDate;

public interface ListingStatisticsService {

    ListingStatisticsOverview getOverview(LocalDate since);
}
