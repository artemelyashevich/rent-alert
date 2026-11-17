package com.rentalert.roommate_ms.cleanup;

import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.kafka.cleanup.AbstractCleanupParticipant;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Answers the cleanup saga step of roommate-ms, so the saga does not wait for a timeout. The saga command carries no
 * user id, so a user's roommate data is not removed here: it is removed by {@code RoommateUserDataService} on the
 * USER_DELETED event.
 */
@Component
public class RoommateCleanupParticipant extends AbstractCleanupParticipant {

    public RoommateCleanupParticipant(@Value("${spring.application.name}") String serviceName) {
        super(serviceName);
    }

    @Override
    protected CleanupResult cleanUp(Instant referenceTime) {
        return CleanupResult.empty();
    }
}
