package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.RoommateCandidateView;
import com.rentalert.roommate_ms.dto.RoommateFeedPage;
import com.rentalert.roommate_ms.dto.RoommateListingCardView;

public interface RoommateFeedService {

    /**
     * A page of people matching the viewer's city, budget, move-in dates and gender preferences, ordered by
     * compatibility score, then newest first. Hidden people and people already in a pair with the viewer are left
     * out; only the 200 newest matches are ranked. A cursor issued before the viewer edited their profile restarts the
     * feed.
     */
    RoommateFeedPage<RoommateCandidateView> candidates(Long viewerId, String cursor, Integer size);

    /**
     * A page of active room listings in the viewer's city that fit their budget, gender and age, ranked like
     * {@link #candidates}.
     */
    RoommateFeedPage<RoommateListingCardView> listings(Long viewerId, String cursor, Integer size);
}
