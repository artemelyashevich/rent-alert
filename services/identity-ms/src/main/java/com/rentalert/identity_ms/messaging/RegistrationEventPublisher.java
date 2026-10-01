package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RegistrationEventPublisher {

    private static final String PRODUCER = "identity-ms";

    private final OutboxPublisher outboxPublisher;

    public void publishRegistered(UserProfile profile) {
        String key = String.valueOf(profile.getId());

        UserActionEvent event = UserActionEvent.builder()
            .event(UserEvent.USER_REGISTERED)
            .build();
        event.setInitiatorId(profile.getId());
        event.setEventStatus(EventStatus.SUCCESS);
        outboxPublisher.publish(KafkaTopics.USER_EVENTS, key, event);

        String name = StringUtils.hasText(profile.getUsername()) ? profile.getUsername() : key;
        NotificationRequest adminAlert = NotificationRequest.builder()
            .producer(PRODUCER)
            .userId(profile.getId())
            .type(NotificationType.USER_CREATED)
            .templateParams(Map.of("message", name))
            .deduplicationKey("user-created:" + key)
            .build();
        outboxPublisher.publish(KafkaTopics.notificationRequests(NotificationType.USER_CREATED), key, adminAlert);
    }
}
