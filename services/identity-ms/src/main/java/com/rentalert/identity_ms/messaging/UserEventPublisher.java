package com.rentalert.identity_ms.messaging;

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

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(UserEvent userEvent, Long userId) {
        publish(userEvent, userId, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(UserEvent userEvent, Long userId, Long filterId) {
        UserActionEvent event = UserActionEvent.builder()
            .event(userEvent)
            .filterId(filterId)
            .build();
        event.setInitiatorId(userId);
        event.setEventStatus(EventStatus.SUCCESS);
        outboxPublisher.publish(KafkaTopics.USER_EVENTS, String.valueOf(userId), event);
    }
}
