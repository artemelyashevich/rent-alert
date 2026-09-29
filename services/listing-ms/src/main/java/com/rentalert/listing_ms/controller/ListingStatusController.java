package com.rentalert.listing_ms.controller;

import com.rentalert.listing_ms.config.PublicUrls;
import com.rentalert.listing_ms.dto.ListingStatusResponseDto;
import com.rentalert.listing_ms.service.ListingStatusService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.LISTING_STATUS_TAG)
@RequiredArgsConstructor
public class ListingStatusController {

    private final ListingStatusService listingStatusService;

    @GetMapping
    public List<ListingStatusResponseDto> findAll() {
        return listingStatusService.findAll();
    }
}
