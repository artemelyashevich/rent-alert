package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserEventPublisher extends AbstractOutboxEventPublisher {

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(UserEvent userEvent, Long userId) {
        publish(userEvent, userId, null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(UserEvent userEvent, Long userId, Long filterId) {
        publishEvent(KafkaTopics.USER_EVENTS, userId, UserActionEvent.builder()
            .event(userEvent)
            .filterId(filterId)
            .build());
    }
}
