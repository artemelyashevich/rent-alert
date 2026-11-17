package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.repository.RoommateListingCleanupRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateListingCleanupService {

    private final RoommateListingCleanupRepository repository;

    /**
     * Removes shared favorites of the given listings and clears their listing id in roommate listings and viewings,
     * in one transaction. Viewings keep their address snapshot. An empty input changes nothing.
     */
    @Transactional
    public void deleteListingReferences(List<Long> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return;
        }
        repository.deleteSharedFavoritesByListingIdIn(listingIds);
        repository.clearListingIdInListings(listingIds);
        repository.clearListingIdInViewings(listingIds);
    }
}
