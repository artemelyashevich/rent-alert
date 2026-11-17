package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends user actions to ops-ms through the transactional outbox: the event is stored in the same transaction as the business change, so it is delivered only if that change is committed.
 */
@Component
public class OpsEventPublisher extends AbstractOutboxEventPublisher {

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishUserEvent(UserEvent userEvent, Long userId, Object... params) {
        UserActionEvent event = UserActionEvent.builder()
            .event(userEvent)
            .params(Arrays.stream(params).map(String::valueOf).toList())
            .build();
        publishEvent(KafkaTopics.USER_EVENTS, userId, event);
    }
}
