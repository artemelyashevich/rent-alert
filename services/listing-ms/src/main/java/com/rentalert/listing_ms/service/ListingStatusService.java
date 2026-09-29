package com.rentalert.listing_ms.service;

import com.rentalert.listing_ms.domain.ListingStatus;
import com.rentalert.listing_ms.dto.ListingStatusResponseDto;
import java.util.List;

public interface ListingStatusService {

    List<ListingStatusResponseDto> findAll();

    ListingStatus findById(Long id);
}
