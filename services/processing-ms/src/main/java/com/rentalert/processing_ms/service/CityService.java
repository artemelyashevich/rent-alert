package com.rentalert.processing_ms.service;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.City;
import java.util.List;

public interface CityService {

    List<City> findActiveByProvider(ScheduleProvider provider);

    List<String> findDistinctNames();
}
