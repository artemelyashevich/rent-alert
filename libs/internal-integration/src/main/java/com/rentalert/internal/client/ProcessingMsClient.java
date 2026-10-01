package com.rentalert.internal.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.currency.CurrencyPair;
import com.rentalert.dto.currency.CurrencyRateDto;
import com.rentalert.dto.listing.CityRequest;
import com.rentalert.dto.listing.CityResponse;
import com.rentalert.dto.listing.CityUpdateRequest;
import com.rentalert.dto.listing.SchedulerSettingsRequest;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.dto.statistics.ListingStatisticsOverview;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ProcessingMsClient.SERVICE_NAME, path = "/processing/internal")
public interface ProcessingMsClient {

    String SERVICE_NAME = "processing-ms";

    @GetMapping("/currency-rates/{pair}")
    CurrencyRateDto getLatestRate(@PathVariable("pair") CurrencyPair pair);

    @GetMapping("/admin/cities")
    PageResponse<CityResponse> findCities(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @GetMapping("/admin/cities/{id}")
    CityResponse findCity(@PathVariable("id") Long id);

    @PostMapping("/admin/cities")
    CityResponse createCity(@RequestBody CityRequest request);

    @PutMapping("/admin/cities/{id}")
    CityResponse updateCity(@PathVariable("id") Long id, @RequestBody CityUpdateRequest request);

    @PutMapping("/admin/cities/{id}/active")
    CityResponse setCityActive(@PathVariable("id") Long id, @RequestParam("active") boolean active);

    @DeleteMapping("/admin/cities/{id}")
    void deleteCity(@PathVariable("id") Long id);

    @GetMapping("/admin/scheduler-settings")
    List<SchedulerSettingsResponse> findSchedulerSettings();

    @GetMapping("/admin/scheduler-settings/{providerName}")
    SchedulerSettingsResponse findSchedulerSettings(@PathVariable("providerName") String providerName);

    @PostMapping("/admin/scheduler-settings")
    SchedulerSettingsResponse createSchedulerSettings(@RequestBody SchedulerSettingsRequest request);

    @PutMapping("/admin/scheduler-settings")
    SchedulerSettingsResponse updateSchedulerSettings(@RequestBody SchedulerSettingsRequest request);

    @DeleteMapping("/admin/scheduler-settings/{providerName}")
    void deleteSchedulerSettings(@PathVariable("providerName") String providerName);

    @PostMapping("/admin/jobs/currency-rates")
    void syncCurrencyRates();

    @GetMapping("/admin/listing-statistics/overview")
    ListingStatisticsOverview getListingStatistics(@RequestParam("since") String since);
}
