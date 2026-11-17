package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RegistrationEventPublisher extends AbstractOutboxEventPublisher {

    private static final String PRODUCER = "identity-ms";

    public void publishRegistered(UserProfile profile) {
        String key = String.valueOf(profile.getId());

        publishEvent(KafkaTopics.USER_EVENTS, profile.getId(), UserActionEvent.builder()
            .event(UserEvent.USER_REGISTERED)
            .build());

        String name = StringUtils.hasText(profile.getUsername()) ? profile.getUsername() : key;
        NotificationRequest adminAlert = NotificationRequest.builder()
            .producer(PRODUCER)
            .userId(profile.getId())
            .type(NotificationType.USER_CREATED)
            .templateParams(Map.of("message", name))
            .deduplicationKey("user-created:" + key)
            .build();
        publish(KafkaTopics.notificationRequests(NotificationType.USER_CREATED), key, adminAlert);
    }
}
