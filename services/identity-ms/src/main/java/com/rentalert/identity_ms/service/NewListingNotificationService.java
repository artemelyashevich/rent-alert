package com.rentalert.identity_ms.service;

import com.rentalert.dto.event.listing.ListingCreatedEvent;

public interface NewListingNotificationService {

    /**
     * Finds the users whose active filters match the listing and asks notification-ms to notify each of them once.
     */
    void notifyMatchingUsers(ListingCreatedEvent listing);
}
