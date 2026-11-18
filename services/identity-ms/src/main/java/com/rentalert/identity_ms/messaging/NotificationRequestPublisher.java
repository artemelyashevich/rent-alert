package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class NotificationRequestPublisher extends AbstractOutboxEventPublisher {

    public void publishRequest(NotificationRequest request) {
        publish(KafkaTopics.notificationRequests(request.type()), String.valueOf(request.userId()), request);
    }

    public void publishListingMatch(NotificationRequest request) {
        publish(KafkaTopics.NOTIFICATION_REQUESTS, String.valueOf(request.userId()), request);
    }
}
