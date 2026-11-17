package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.identity_ms.service.BotStatusService;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "identity.user-events";

    private final BotStatusService botStatusService;

    public UserEventListener(BotStatusService botStatusService) {
        super(CONSUMER);
        this.botStatusService = botStatusService;
    }

    /**
     * Applies bot block and unblock actions once per event idempotency key; other user actions are ignored.
     */
    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = CONSUMER)
    public void onUserAction(UserActionEvent event) {
        if (event.getInitiatorId() == null) {
            return;
        }
        processOnce(event.getIdempotencyKey(), () -> {
            if (event.getEvent() == UserEvent.BOT_BLOCKED) {
                botStatusService.markBotBlocked(event.getInitiatorId());
            } else if (event.getEvent() == UserEvent.BOT_UNBLOCKED) {
                botStatusService.markBotUnblocked(event.getInitiatorId());
            }
        });
    }
}
