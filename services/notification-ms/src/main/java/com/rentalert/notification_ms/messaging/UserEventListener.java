package com.rentalert.notification_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.notification_ms.service.NotificationSettingsService;
import com.rentalert.notification_ms.service.UserDataService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "notification.user-events";

    private final NotificationSettingsService notificationSettingsService;
    private final UserDataService userDataService;

    public UserEventListener(NotificationSettingsService notificationSettingsService, UserDataService userDataService) {
        super(CONSUMER);
        this.notificationSettingsService = notificationSettingsService;
        this.userDataService = userDataService;
    }

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = CONSUMER)
    public void onUserAction(UserActionEvent event) {
        if (event.getInitiatorId() == null) {
            return;
        }
        processOnce(event.getIdempotencyKey(), () -> {
            if (event.getEvent() == UserEvent.USER_DELETED) {
                userDataService.deleteUserData(event.getInitiatorId());
            } else if (event.getEvent() == UserEvent.USER_REGISTERED) {
                notificationSettingsService.createDefault(event.getInitiatorId());
            }
        });
    }
}
