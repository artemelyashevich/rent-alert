package com.rentalert.notification_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.notification_ms.service.NotificationSettingsService;
import com.rentalert.notification_ms.service.UserDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final UserDataService userDataService;
    private final NotificationSettingsService notificationSettingsService;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "notification.user-events")
    public void onUserAction(UserActionEvent event) {
        if (event.getInitiatorId() == null) {
            return;
        }
        if (event.getEvent() == UserEvent.USER_DELETED) {
            userDataService.deleteUserData(event.getInitiatorId());
        } else if (event.getEvent() == UserEvent.USER_REGISTERED) {
            notificationSettingsService.createDefault(event.getInitiatorId());
        }
    }
}
