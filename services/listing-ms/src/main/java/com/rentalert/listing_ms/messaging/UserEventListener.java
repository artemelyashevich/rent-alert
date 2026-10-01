package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.listing_ms.service.UserDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final UserDataService userDataService;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "listing.user-events")
    public void onUserAction(UserActionEvent event) {
        if (event.getEvent() == UserEvent.USER_DELETED && event.getInitiatorId() != null) {
            userDataService.deleteUserData(event.getInitiatorId());
        }
    }
}
