package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.listing_ms.service.UserDataService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "listing.user-events";

    private final UserDataService userDataService;

    public UserEventListener(UserDataService userDataService) {
        super(CONSUMER);
        this.userDataService = userDataService;
    }

    /**
     * Removes listing data of a deleted user; the removal runs once per event. Other user events are ignored.
     */
    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = CONSUMER)
    public void onUserAction(UserActionEvent event) {
        if (event.getEvent() == UserEvent.USER_DELETED && event.getInitiatorId() != null) {
            processOnce(event.getIdempotencyKey(), () -> userDataService.deleteUserData(event.getInitiatorId()));
        }
    }
}
