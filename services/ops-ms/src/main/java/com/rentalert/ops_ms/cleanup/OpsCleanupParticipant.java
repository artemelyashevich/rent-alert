package com.rentalert.ops_ms.cleanup;

import com.rentalert.common.util.BatchDeleter;
import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.kafka.cleanup.AbstractCleanupParticipant;
import com.rentalert.ops_ms.repository.AuditLogRepository;
import com.rentalert.ops_ms.repository.UserStatisticsRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OpsCleanupParticipant extends AbstractCleanupParticipant {

    private final AuditLogRepository auditLogRepository;
    private final UserStatisticsRepository userStatisticsRepository;
    private final TransactionTemplate transactionTemplate;
    private final Duration auditLogRetention;
    private final Period statisticsRetention;
    private final ZoneId zone;
    private final int maxRowsPerRun;

    public OpsCleanupParticipant(AuditLogRepository auditLogRepository, UserStatisticsRepository userStatisticsRepository, TransactionTemplate transactionTemplate,
                                 @Value("${app.cleanup.audit-log-retention:P3D}") Duration auditLogRetention, @Value("${app.cleanup.statistics-retention:P7D}") Period statisticsRetention,
                                 @Value("${app.bot.timezone:Europe/Minsk}") ZoneId zone, @Value("${app.cleanup.max-rows-per-run:20000}") int maxRowsPerRun) {
        this.auditLogRepository = auditLogRepository;
        this.userStatisticsRepository = userStatisticsRepository;
        this.transactionTemplate = transactionTemplate;
        this.auditLogRetention = auditLogRetention;
        this.statisticsRetention = statisticsRetention;
        this.zone = zone;
        this.maxRowsPerRun = maxRowsPerRun;
    }

    @Override
    public CleanupResult cleanUp(Instant referenceTime) {
        return deleteAuditLog(referenceTime).plus(deleteStatistics(referenceTime));
    }

    public CleanupResult deleteAuditLog(Instant referenceTime) {
        return BatchDeleter.delete("audit-log", () -> auditLogRepository.findIdsCreatedBefore(referenceTime.minus(auditLogRetention), PageRequest.of(0, maxRowsPerRun)),
            batch -> transactionTemplate.execute(status -> auditLogRepository.deleteByIdIn(batch)));
    }

    public CleanupResult deleteStatistics(Instant referenceTime) {
        LocalDate cutoff = LocalDate.ofInstant(referenceTime, zone).minus(statisticsRetention);
        return BatchDeleter.delete("user-statistics", () -> userStatisticsRepository.findIdsByEventDateBefore(cutoff, PageRequest.of(0, maxRowsPerRun)),
            batch -> transactionTemplate.execute(status -> userStatisticsRepository.deleteByIdIn(batch)));
    }
}
