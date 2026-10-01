package com.rentalert.notification_ms.messaging;

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

@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final OutboxPublisher outboxPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishBotBlocked(Long userId) {
        publish(UserEvent.BOT_BLOCKED, userId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishSettingsUpdated(Long userId) {
        publish(UserEvent.NOTIFICATION_SETTINGS_UPDATED, userId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishBotUnblocked(Long userId) {
        publish(UserEvent.BOT_UNBLOCKED, userId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishBotCommand(Long userId, Object command) {
        publish(UserEvent.BOT_COMMAND, userId, command);
    }

    @Transactional
    public void publishBotAction(UserEvent userEvent, Long userId, Object... params) {
        publish(userEvent, userId, params);
    }

    private void publish(UserEvent userEvent, Long userId, Object... params) {
        UserActionEvent event = UserActionEvent.builder()
            .event(userEvent)
            .params(Arrays.stream(params).map(String::valueOf).toList())
            .build();
        event.setInitiatorId(userId);
        event.setEventStatus(EventStatus.SUCCESS);
        outboxPublisher.publish(KafkaTopics.USER_EVENTS, String.valueOf(userId), event);
    }
}
