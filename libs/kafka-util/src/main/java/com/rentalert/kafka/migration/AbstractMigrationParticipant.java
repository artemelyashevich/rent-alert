package com.rentalert.kafka.migration;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.migration.MigrationCommand;
import com.rentalert.dto.event.migration.MigrationProgress;
import com.rentalert.dto.event.migration.MigrationReply;
import com.rentalert.dto.migration.TableResult;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public abstract class AbstractMigrationParticipant {

    private static final Duration LOCK_LEASE = Duration.ofHours(1);

    private static final int MAX_ERROR_LENGTH = 500;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private MigrationControl migrationControl;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final String serviceName;

    protected AbstractMigrationParticipant(String serviceName) {
        this.serviceName = serviceName;
    }

    protected abstract List<TableResult> migrate(MigrationContext context);

    @KafkaListener(topics = KafkaTopics.MIGRATION_COMMANDS, groupId = "${spring.application.name}.migration")
    public void onCommand(MigrationCommand command) {
        if (!serviceName.equals(command.service())) {
            return;
        }
        String lockName = "migration:" + command.migrationId() + ":" + serviceName;
        boolean started = migrationControl.tryWithLock(lockName, LOCK_LEASE, () -> runMigration(command));
        if (!started) {
            log.warn("[MIGRATION] {} step {} is already running on this instance, command skipped", command.migrationId(), command.step());
        }
    }

    private void runMigration(MigrationCommand command) {
        log.info("[MIGRATION] {} step {} started", command.migrationId(), command.step());
        MigrationContext context = new MigrationContext(command.migrationId(), command.step(), serviceName,
            command.settings(), migrationControl, this::sendProgress);
        MigrationReply reply;
        try {
            List<TableResult> tables = migrate(context);
            reply = new MigrationReply(command.migrationId(), command.step(), serviceName, true, tables, null);
            log.info("[MIGRATION] {} step {} finished: tables={}", command.migrationId(), command.step(), tables.size());
        } catch (RuntimeException e) {
            String error = errorMessage(e);
            log.error("[MIGRATION] {} step {} failed: {}", command.migrationId(), command.step(), error);
            reply = new MigrationReply(command.migrationId(), command.step(), serviceName, false, List.of(), error);
        }
        MigrationReply finalReply = reply;
        transactionTemplate.executeWithoutResult(status ->
            outboxPublisher.publish(KafkaTopics.MIGRATION_REPLIES, command.migrationId().toString(), finalReply));
    }

    private void sendProgress(MigrationProgress progress) {
        try {
            kafkaTemplate.send(KafkaTopics.MIGRATION_PROGRESS, progress.migrationId().toString(), progress)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        log.warn("[MIGRATION] Failed to send progress of {}: {}", progress.migrationId(), e.getMessage());
                    }
                });
        } catch (RuntimeException e) {
            log.warn("[MIGRATION] Failed to send progress of {}: {}", progress.migrationId(), e.getMessage());
        }
    }

    private static String errorMessage(RuntimeException e) {
        String text = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }
}
