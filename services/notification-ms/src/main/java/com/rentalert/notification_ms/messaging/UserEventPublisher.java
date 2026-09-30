package com.rentalert.notification_ms.messaging;

import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final OutboxPublisher outboxPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishBotBlocked(Long userId) {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.BOT_BLOCKED).build();
        event.setInitiatorId(userId);
        event.setEventStatus(EventStatus.SUCCESS);
        outboxPublisher.publish(KafkaTopics.USER_EVENTS, String.valueOf(userId), event);
    }
}
