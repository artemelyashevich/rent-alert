package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import java.util.List;

public interface RoommateScoringService {

    int score(RoommateProfile viewer, RoommateProfile candidate);

    int scoreListing(RoommateProfile viewer, RoommateListing listing);

    List<String> sharedTags(RoommateProfile viewer, RoommateProfile candidate);
}
