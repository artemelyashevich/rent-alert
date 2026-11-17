package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.dto.RoommateListingRequest;
import com.rentalert.roommate_ms.dto.RoommateListingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoommateListingService {

    /**
     * Creates a room listing. It becomes {@code ACTIVE} right away when its description passes the auto-filter,
     * otherwise it waits for manual moderation. A linked aggregator listing must exist in listing-ms; an author may
     * have only one active listing.
     */
    RoommateListingResponse create(Long authorUserId, RoommateListingRequest request);

    RoommateListingResponse update(Long listingId, Long authorUserId, RoommateListingRequest request);

    /**
     * Changes the status on the author's behalf: only {@code ACTIVE}, {@code PAUSED} and {@code ARCHIVED} are
     * allowed, and {@code ACTIVE} needs an approved description and no other active listing of the author.
     */
    RoommateListingResponse setStatus(Long listingId, Long authorUserId, RoommateListingStatus status);

    Page<RoommateListingResponse> findMine(Long authorUserId, Pageable pageable);

    RoommateListingResponse getOwned(Long listingId, Long authorUserId);
}
