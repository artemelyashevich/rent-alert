package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends user actions to ops-ms through the transactional outbox: the event is stored in the same transaction
 * as the business change, so it is delivered only if that change is committed.
 */
@Component
@RequiredArgsConstructor
public class OpsEventPublisher {

    private final OutboxPublisher outboxPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishUserEvent(UserEvent userEvent, Long userId, Object... params) {
        UserActionEvent event = UserActionEvent.builder()
            .event(userEvent)
            .params(Arrays.stream(params).map(String::valueOf).toList())
            .build();
        event.setInitiatorId(userId);
        event.setEventStatus(EventStatus.SUCCESS);
        outboxPublisher.publish(KafkaTopics.USER_EVENTS, String.valueOf(userId), event);
    }
}
