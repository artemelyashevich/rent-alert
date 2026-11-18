package com.rentalert.roommate_ms.cleanup;

import com.rentalert.common.util.BatchDeleter;
import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.kafka.cleanup.AbstractCleanupParticipant;
import com.rentalert.roommate_ms.repository.RoommateRetentionCleanupRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class RoommateCleanupParticipant extends AbstractCleanupParticipant {

    private final int maxRowsPerRun;
    private final Duration retention;
    private final RoommateRetentionCleanupRepository retentionCleanupRepository;
    private final TransactionTemplate transactionTemplate;

    public RoommateCleanupParticipant(@Value("${spring.application.name}") String serviceName,
                                      @Value("${app.cleanup.max-rows-per-run:20000}") int maxRowsPerRun,
                                      @Value("${app.cleanup.roommate-retention:P180D}") Duration retention,
                                      RoommateRetentionCleanupRepository retentionCleanupRepository,
                                      TransactionTemplate transactionTemplate) {
        super(serviceName);
        this.maxRowsPerRun = maxRowsPerRun;
        this.retention = retention;
        this.retentionCleanupRepository = retentionCleanupRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    protected CleanupResult cleanUp(Instant referenceTime) {
        Instant cutoff = referenceTime.minus(retention);
        return CleanupResult.empty()
            .plus(deleteMatchEvents(cutoff))
            .plus(deleteViewings(cutoff))
            .plus(deleteResolvedModerationTasks(cutoff));
    }

    private CleanupResult deleteMatchEvents(Instant cutoff) {
        return BatchDeleter.delete("roommate-match-events",
            () -> retentionCleanupRepository.findStaleMatchEventIds(cutoff, maxRowsPerRun),
            batch -> transactionTemplate.execute(status -> retentionCleanupRepository.deleteMatchEventsByIdIn(batch)));
    }

    private CleanupResult deleteViewings(Instant cutoff) {
        return BatchDeleter.delete("roommate-viewings",
            () -> retentionCleanupRepository.findStaleViewingIds(cutoff, maxRowsPerRun),
            batch -> transactionTemplate.execute(status -> retentionCleanupRepository.deleteViewingsByIdIn(batch)));
    }

    private CleanupResult deleteResolvedModerationTasks(Instant cutoff) {
        return BatchDeleter.delete("roommate-moderation-tasks",
            () -> retentionCleanupRepository.findResolvedModerationTaskIds(cutoff, maxRowsPerRun),
            batch -> transactionTemplate.execute(status -> retentionCleanupRepository.deleteModerationTasksByIdIn(batch)));
    }
}
