package com.rentalert.roommate_ms.service;

public interface RoommateHiddenService {

    /**
     * Hides a person from the viewer's candidate feed; hiding twice is a no-op.
     */
    void skipCandidate(Long viewerId, Long candidateUserId);

    /**
     * Hides a room listing from the viewer's listing feed; hiding twice is a no-op.
     */
    void skipListing(Long viewerId, Long listingId);
}
