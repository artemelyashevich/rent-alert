package com.rentalert.roommate_ms.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.dto.cleanup.CleanupResult;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoommateCleanupParticipantTests {

    @Test
    void repliesWithEmptyResultWithoutDeletingUserData() {
        CleanupResult result = new RoommateCleanupParticipant("roommate-ms").cleanUp(Instant.now());

        assertThat(result.deleted()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(result.hasFailures()).isFalse();
    }
}
