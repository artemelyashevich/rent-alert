package com.rentalert.kafka.cleanup;

import com.rentalert.dto.cleanup.CleanupResult;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.cleanup.CleanupCommand;
import com.rentalert.dto.event.cleanup.CleanupReply;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;

@Slf4j
public abstract class AbstractCleanupParticipant extends AbstractIdempotentKafkaListener {

    @Autowired
    private OutboxPublisher outboxPublisher;

    private final String serviceName;

    protected AbstractCleanupParticipant(String serviceName) {
        super(serviceName);
        this.serviceName = serviceName;
    }

    protected abstract CleanupResult cleanUp(Instant referenceTime);

    @KafkaListener(topics = KafkaTopics.CLEANUP_COMMANDS, groupId = "${spring.application.name}.cleanup")
    public void onCommand(CleanupCommand command) {
        if (!serviceName.equals(command.service())) {
            return;
        }
        processOnce(commandKey(command), () -> {
            log.info("[CLEANUP] saga {} step {} started", command.sagaId(), command.step());
            CleanupResult result = cleanUp(command.referenceTime());
            outboxPublisher.publish(KafkaTopics.CLEANUP_REPLIES, command.sagaId().toString(), CleanupReply.of(command, result));
            log.info("[CLEANUP] saga {} step {} finished: deleted={} failed={} time={}ms", command.sagaId(), command.step(), result.deleted(), result.failed(), result.duration().toMillis());
        });
    }

    private static UUID commandKey(CleanupCommand command) {
        return UUID.nameUUIDFromBytes(("cleanup:" + command.sagaId() + ":" + command.step() + ":" + command.service()).getBytes(StandardCharsets.UTF_8));
    }
}
