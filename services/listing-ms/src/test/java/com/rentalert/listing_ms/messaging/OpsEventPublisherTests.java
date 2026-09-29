package com.rentalert.listing_ms.messaging;

import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OpsEventPublisherTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final OpsEventPublisher publisher = new OpsEventPublisher(outboxPublisher);

    @Test
    void publishesUserEventKeyedByUser() {
        publisher.publishUserEvent(UserEvent.FAVORITE_ADDED, 42L, 15L);

        verify(outboxPublisher).publish(eq(KafkaTopics.USER_EVENTS), eq("42"), argThat((UserActionEvent event) ->
            event.getEvent() == UserEvent.FAVORITE_ADDED
                && event.getInitiatorId() == 42L
                && event.getParams().equals(List.of("15"))
                && event.getEventStatus() == EventStatus.SUCCESS));
    }
}
