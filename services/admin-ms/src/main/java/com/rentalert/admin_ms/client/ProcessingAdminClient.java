package com.rentalert.admin_ms.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.listing.CityRequest;
import com.rentalert.dto.listing.CityResponse;
import com.rentalert.dto.listing.CityUpdateRequest;
import com.rentalert.dto.listing.SchedulerSettingsRequest;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "processing-ms", contextId = "processingAdminClient", path = "/processing/internal/admin")
public interface ProcessingAdminClient {

    @GetMapping("/cities")
    PageResponse<CityResponse> findCities(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @GetMapping("/cities/{id}")
    CityResponse findCity(@PathVariable("id") Long id);

    @PostMapping("/cities")
    CityResponse createCity(@RequestBody CityRequest request);

    @PutMapping("/cities/{id}")
    CityResponse updateCity(@PathVariable("id") Long id, @RequestBody CityUpdateRequest request);

    @PutMapping("/cities/{id}/active")
    CityResponse setCityActive(@PathVariable("id") Long id, @RequestParam("active") boolean active);

    @DeleteMapping("/cities/{id}")
    void deleteCity(@PathVariable("id") Long id);

    @GetMapping("/scheduler-settings")
    List<SchedulerSettingsResponse> findSchedulerSettings();

    @GetMapping("/scheduler-settings/{providerName}")
    SchedulerSettingsResponse findSchedulerSettings(@PathVariable("providerName") String providerName);

    @PostMapping("/scheduler-settings")
    SchedulerSettingsResponse createSchedulerSettings(@RequestBody SchedulerSettingsRequest request);

    @PutMapping("/scheduler-settings")
    SchedulerSettingsResponse updateSchedulerSettings(@RequestBody SchedulerSettingsRequest request);

    @DeleteMapping("/scheduler-settings/{providerName}")
    void deleteSchedulerSettings(@PathVariable("providerName") String providerName);
}
