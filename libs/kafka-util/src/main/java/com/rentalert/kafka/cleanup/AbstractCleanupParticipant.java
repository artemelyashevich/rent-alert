package com.rentalert.kafka.cleanup;

import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.cleanup.CleanupCommand;
import com.rentalert.dto.event.cleanup.CleanupReply;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public abstract class AbstractCleanupParticipant {

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final String serviceName;

    protected AbstractCleanupParticipant(String serviceName) {
        this.serviceName = serviceName;
    }

    protected abstract CleanupResult cleanUp(Instant referenceTime);

    @KafkaListener(topics = KafkaTopics.CLEANUP_COMMANDS, groupId = "${spring.application.name}.cleanup")
    public void onCommand(CleanupCommand command) {
        if (!serviceName.equals(command.service())) {
            return;
        }
        log.info("[CLEANUP] saga {} step {} started", command.sagaId(), command.step());
        CleanupResult result = cleanUp(command.referenceTime());
        CleanupReply reply = CleanupReply.of(command, result);
        transactionTemplate.executeWithoutResult(status -> outboxPublisher.publish(KafkaTopics.CLEANUP_REPLIES, command.sagaId().toString(), reply));
        log.info("[CLEANUP] saga {} step {} finished: deleted={} failed={} time={}ms", command.sagaId(), command.step(), result.deleted(), result.failed(), result.duration().toMillis());
    }
}
