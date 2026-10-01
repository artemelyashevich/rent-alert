package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.client.ProcessingAdminClient;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.dto.statistics.ListingStatisticsOverview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin listing statistics", description = "Статистика объявлений и здоровье парсеров")
@RestController
@RequestMapping(PublicUrls.LISTING_STATISTICS_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminListingStatisticsController {

    private final ProcessingAdminClient processingAdminClient;

    @Operation(summary = "Обзор объявлений с начала периода since")
    @GetMapping("/overview")
    public ListingStatisticsOverview getOverview(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate since) {
        return processingAdminClient.getListingStatistics(since.toString());
    }
}
