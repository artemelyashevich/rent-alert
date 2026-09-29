package com.rentalert.processing_ms.repository;

import com.rentalert.processing_ms.domain.City;
import com.rentalert.dto.listing.ScheduleProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findAllByProvider(ScheduleProvider provider);

    List<City> findAllByProviderAndActiveTrue(ScheduleProvider provider);

    Optional<City> findByProviderAndCityCode(ScheduleProvider provider, String cityCode);

    @Query("SELECT DISTINCT c.name FROM City c ORDER BY c.name")
    List<String> findDistinctNames();
}
