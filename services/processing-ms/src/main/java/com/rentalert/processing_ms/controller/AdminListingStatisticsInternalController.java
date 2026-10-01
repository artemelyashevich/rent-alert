package com.rentalert.processing_ms.controller;

import com.rentalert.dto.statistics.ListingStatisticsOverview;
import com.rentalert.processing_ms.config.PublicUrls;
import com.rentalert.processing_ms.service.ListingStatisticsService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG + "/listing-statistics")
@RequiredArgsConstructor
public class AdminListingStatisticsInternalController {

    private final ListingStatisticsService listingStatisticsService;

    @GetMapping("/overview")
    public ListingStatisticsOverview getOverview(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate since) {
        return listingStatisticsService.getOverview(since);
    }
}
