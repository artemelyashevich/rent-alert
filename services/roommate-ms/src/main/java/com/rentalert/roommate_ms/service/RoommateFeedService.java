package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.dto.RoommateCandidateView;
import com.rentalert.roommate_ms.dto.RoommateFeedPage;
import com.rentalert.roommate_ms.dto.RoommateListingCardView;

public interface RoommateFeedService {

    RoommateFeedPage<RoommateCandidateView> candidates(Long viewerId, String cursor, Integer size);

    RoommateFeedPage<RoommateListingCardView> listings(Long viewerId, String cursor, Integer size);
}
