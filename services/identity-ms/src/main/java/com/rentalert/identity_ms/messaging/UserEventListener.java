package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.identity_ms.service.BotStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final BotStatusService botStatusService;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "identity.user-events")
    public void onUserAction(UserActionEvent event) {
        if (event.getEvent() == UserEvent.BOT_BLOCKED && event.getInitiatorId() != null) {
            botStatusService.markBotBlocked(event.getInitiatorId());
        }
    }
}
