package com.rentalert.identity_ms.messaging;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.kafka.outbox.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationRequestPublisherTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final NotificationRequestPublisher publisher = createPublisher();

    private NotificationRequestPublisher createPublisher() {
        NotificationRequestPublisher created = new NotificationRequestPublisher();
        ReflectionTestUtils.setField(created, "outboxPublisher", outboxPublisher);
        return created;
    }

    private static NotificationRequest request(NotificationType type) {
        return NotificationRequest.builder().userId(5L).type(type).build();
    }

    @Test
    void requestGoesToTopicOfItsType() {
        NotificationRequest request = request(NotificationType.EMAIL_ACTIVATION);

        publisher.publishRequest(request);

        verify(outboxPublisher).publish(eq(KafkaTopics.notificationRequests(NotificationType.EMAIL_ACTIVATION)), eq("5"), eq(request));
    }

    @Test
    void listingMatchGoesToRegularNotificationTopic() {
        NotificationRequest request = request(NotificationType.NEW_LISTING_MATCHED);

        publisher.publishListingMatch(request);

        verify(outboxPublisher).publish(eq(KafkaTopics.NOTIFICATION_REQUESTS), eq("5"), eq(request));
    }
}
