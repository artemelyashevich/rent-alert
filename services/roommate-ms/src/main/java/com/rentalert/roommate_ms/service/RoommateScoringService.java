package com.rentalert.roommate_ms.service;

import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import java.util.List;

public interface RoommateScoringService {

    /**
     * Compatibility of two people from 0 to 100: shared lifestyle tags, schedule, cleanliness, smoking and budget
     * overlap. Missing answers count as neutral.
     */
    int score(RoommateProfile viewer, RoommateProfile candidate);

    /**
     * How well a room listing suits the viewer from 0 to 100: house rules against lifestyle tags, rent against
     * budget, the viewer's age against the listing's age range.
     */
    int scoreListing(RoommateProfile viewer, RoommateListing listing);

    List<String> sharedTags(RoommateProfile viewer, RoommateProfile candidate);
}
