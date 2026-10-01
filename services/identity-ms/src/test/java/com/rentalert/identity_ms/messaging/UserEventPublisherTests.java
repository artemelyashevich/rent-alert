package com.rentalert.identity_ms.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.outbox.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserEventPublisherTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final UserEventPublisher publisher = new UserEventPublisher(outboxPublisher);

    @Test
    void filterEventCarriesFilterId() {
        publisher.publish(UserEvent.FILTER_UPDATED, 5L, 9L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.USER_EVENTS), eq("5"), captor.capture());
        UserActionEvent event = (UserActionEvent) captor.getValue();
        assertThat(event.getEvent()).isEqualTo(UserEvent.FILTER_UPDATED);
        assertThat(event.getInitiatorId()).isEqualTo(5L);
        assertThat(event.getFilterId()).isEqualTo(9L);
    }
}
