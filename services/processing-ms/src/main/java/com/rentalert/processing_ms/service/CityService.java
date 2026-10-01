package com.rentalert.processing_ms.service;

import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.City;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CityService {

    Page<City> findAll(Pageable pageable);

    List<City> findActiveByProvider(ScheduleProvider provider);

    List<String> findDistinctNames();

    City findById(Long id);

    City create(ScheduleProvider provider, String name, String cityCode, boolean active);

    City update(Long id, String name, boolean active);

    City setActive(Long id, boolean active);

    void delete(Long id);
}
