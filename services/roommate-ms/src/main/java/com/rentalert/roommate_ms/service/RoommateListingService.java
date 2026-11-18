package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.dto.RoommateListingRequest;
import com.rentalert.roommate_ms.dto.RoommateListingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoommateListingService {

    RoommateListingResponse create(Long authorUserId, RoommateListingRequest request);

    RoommateListingResponse update(Long listingId, Long authorUserId, RoommateListingRequest request);

    RoommateListingResponse setStatus(Long listingId, Long authorUserId, RoommateListingStatus status);

    Page<RoommateListingResponse> findMine(Long authorUserId, Pageable pageable);

    RoommateListingResponse getOwned(Long listingId, Long authorUserId);
}
