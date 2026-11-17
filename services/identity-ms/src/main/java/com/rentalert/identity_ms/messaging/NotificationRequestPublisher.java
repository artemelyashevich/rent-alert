package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Sends notification requests to notification-ms through the outbox, keyed by the recipient user id.
 */
@Component
public class NotificationRequestPublisher extends AbstractOutboxEventPublisher {

    /**
     * Routes the request to the topic of its notification type, which may be a priority topic.
     */
    public void publishRequest(NotificationRequest request) {
        publish(KafkaTopics.notificationRequests(request.type()), String.valueOf(request.userId()), request);
    }

    /**
     * Sends a new-listing match notification to the regular notification topic.
     */
    public void publishListingMatch(NotificationRequest request) {
        publish(KafkaTopics.NOTIFICATION_REQUESTS, String.valueOf(request.userId()), request);
    }
}
