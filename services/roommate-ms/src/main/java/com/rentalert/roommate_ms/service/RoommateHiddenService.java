package com.rentalert.roommate_ms.service;

public interface RoommateHiddenService {

    void skipCandidate(Long viewerId, Long candidateUserId);

    void skipListing(Long viewerId, Long listingId);
}
