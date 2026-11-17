package com.rentalert.notification_ms.cleanup;

import com.rentalert.common.util.BatchDeleter;
import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.kafka.cleanup.AbstractCleanupParticipant;
import com.rentalert.notification_ms.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class NotificationCleanupParticipant extends AbstractCleanupParticipant {

    private final NotificationRepository notificationRepository;
    private final TransactionTemplate transactionTemplate;
    private final Duration notificationRetention;
    private final int maxRowsPerRun;

    public NotificationCleanupParticipant(@Value("${spring.application.name}") String serviceName, NotificationRepository notificationRepository, TransactionTemplate transactionTemplate,
                                          @Value("${app.cleanup.notification-retention:P2D}") Duration notificationRetention, @Value("${app.cleanup.max-rows-per-run:20000}") int maxRowsPerRun) {
        super(serviceName);
        this.notificationRepository = notificationRepository;
        this.transactionTemplate = transactionTemplate;
        this.notificationRetention = notificationRetention;
        this.maxRowsPerRun = maxRowsPerRun;
    }

    @Override
    public CleanupResult cleanUp(Instant referenceTime) {
        return BatchDeleter.delete("notifications", () -> notificationRepository.findIdsCreatedBefore(referenceTime.minus(notificationRetention), PageRequest.of(0, maxRowsPerRun)),
            batch -> transactionTemplate.execute(status -> notificationRepository.deleteByIdIn(batch)));
    }
}
