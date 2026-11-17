package com.rentalert.roommate_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.roommate_ms.service.RoommateUserDataService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "roommate.user-events";

    private final RoommateUserDataService userDataService;

    public UserEventListener(RoommateUserDataService userDataService) {
        super(CONSUMER);
        this.userDataService = userDataService;
    }

    /**
     * Removes all roommate data of a user whose profile was deleted by an administrator.
     */
    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = CONSUMER)
    public void onUserAction(UserActionEvent event) {
        if (event.getEvent() == UserEvent.USER_DELETED && event.getInitiatorId() != null) {
            processOnce(event.getIdempotencyKey(), () -> userDataService.deleteUserData(event.getInitiatorId()));
        }
    }
}
