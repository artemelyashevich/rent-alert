package com.rentalert.internal.client.service;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.dto.listing.CityRequest;
import com.rentalert.dto.listing.CityResponse;
import com.rentalert.dto.listing.CityUpdateRequest;
import com.rentalert.dto.listing.SchedulerSettingsRequest;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.dto.statistics.ListingStatisticsOverview;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface ProcessingMsService {

    Optional<CurrencyRateDto> getLatestRate(CurrencyPair pair);

    PageResponse<CityResponse> findCities(Pageable pageable);

    CityResponse findCity(Long id);

    CityResponse createCity(CityRequest request);

    CityResponse updateCity(Long id, CityUpdateRequest request);

    CityResponse setCityActive(Long id, boolean active);

    void deleteCity(Long id);

    List<SchedulerSettingsResponse> findSchedulerSettings();

    SchedulerSettingsResponse findSchedulerSettings(String providerName);

    SchedulerSettingsResponse createSchedulerSettings(SchedulerSettingsRequest request);

    SchedulerSettingsResponse updateSchedulerSettings(SchedulerSettingsRequest request);

    void deleteSchedulerSettings(String providerName);

    void syncCurrencyRates();

    ListingStatisticsOverview getListingStatistics(LocalDate since);
}
