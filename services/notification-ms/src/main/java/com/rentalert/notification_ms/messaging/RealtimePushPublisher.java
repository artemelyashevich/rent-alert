package com.rentalert.notification_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.RealtimePushEvent;
import com.rentalert.dto.notification.RealtimeNotificationPayload;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RealtimePushPublisher extends AbstractOutboxEventPublisher {

    /**
     * Queues a realtime push to the user's open connections in the outbox of the current transaction.
     */
    public void publishPush(String userId, RealtimeNotificationPayload payload) {
        publish(KafkaTopics.REALTIME_PUSH, userId, new RealtimePushEvent(UUID.randomUUID(), userId, payload));
    }
}
