package com.rentalert.processing_ms.cleanup;

import com.rentalert.common.util.BatchDeleter;
import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.kafka.cleanup.AbstractCleanupParticipant;
import com.rentalert.processing_ms.repository.ListingRepository;
import com.rentalert.processing_ms.repository.ScheduledTaskRecordRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.ToIntFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ProcessingCleanupParticipant extends AbstractCleanupParticipant {

    private final ListingRepository listingRepository;
    private final ScheduledTaskRecordRepository scheduledTaskRecordRepository;
    private final TransactionTemplate transactionTemplate;
    private final Duration listingRetention;
    private final Duration taskRecordRetention;
    private final int maxRowsPerRun;

    public ProcessingCleanupParticipant(ListingRepository listingRepository, ScheduledTaskRecordRepository scheduledTaskRecordRepository, TransactionTemplate transactionTemplate,
                                        @Value("${app.cleanup.listing-retention:P7D}") Duration listingRetention, @Value("${app.cleanup.task-record-retention:P7D}") Duration taskRecordRetention,
                                        @Value("${app.cleanup.max-rows-per-run:20000}") int maxRowsPerRun) {
        this.listingRepository = listingRepository;
        this.scheduledTaskRecordRepository = scheduledTaskRecordRepository;
        this.transactionTemplate = transactionTemplate;
        this.listingRetention = listingRetention;
        this.taskRecordRetention = taskRecordRetention;
        this.maxRowsPerRun = maxRowsPerRun;
    }

    @Override
    public CleanupResult cleanUp(Instant referenceTime) {
        return deleteListings(referenceTime).plus(deleteTaskRecords(referenceTime));
    }

    public CleanupResult deleteListings(Instant referenceTime) {
        return BatchDeleter.delete("listings", () -> listingRepository.findIdsParsedBefore(referenceTime.minus(listingRetention), PageRequest.of(0, maxRowsPerRun)),
            inTransaction(listingRepository::deleteByIdIn));
    }

    public CleanupResult deleteTaskRecords(Instant referenceTime) {
        return BatchDeleter.delete("scheduled-task-records", () -> scheduledTaskRecordRepository.findIdsCreatedBefore(referenceTime.minus(taskRecordRetention), PageRequest.of(0, maxRowsPerRun)),
            inTransaction(scheduledTaskRecordRepository::deleteByIdIn));
    }

    private ToIntFunction<List<Long>> inTransaction(ToIntFunction<List<Long>> delete) {
        return batch -> transactionTemplate.execute(status -> delete.applyAsInt(batch));
    }
}
