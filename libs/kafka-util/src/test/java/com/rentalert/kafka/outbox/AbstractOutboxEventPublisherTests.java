package com.rentalert.kafka.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

class AbstractOutboxEventPublisherTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final AbstractOutboxEventPublisher publisher = new AbstractOutboxEventPublisher() {
    };

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "outboxPublisher", outboxPublisher);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void publishDelegatesTopicKeyAndPayload() {
        Object payload = new Object();

        publisher.publish("topic.test", "key-1", payload);

        verify(outboxPublisher).publish("topic.test", "key-1", payload);
    }

    @Test
    void publishEventSetsInitiatorAndSuccessStatusAndUserKey() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.BOT_BLOCKED).build();

        publisher.publishEvent(KafkaTopics.USER_EVENTS, 42L, event);

        assertThat(event.getInitiatorId()).isEqualTo(42L);
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.SUCCESS);
        verify(outboxPublisher).publish(KafkaTopics.USER_EVENTS, "42", event);
    }

    @Test
    void publishEventKeepsExplicitStatus() {
        UserActionEvent event = UserActionEvent.builder().event(UserEvent.BOT_BLOCKED).build();
        event.setEventStatus(EventStatus.ERROR);

        publisher.publishEvent(KafkaTopics.USER_EVENTS, 42L, event);

        assertThat(event.getEventStatus()).isEqualTo(EventStatus.ERROR);
    }

    @Test
    void outboxPublisherRejectsPublishWithoutActiveTransaction() {
        OutboxPublisher realOutbox = new OutboxPublisher(jdbcTemplate, JsonMapper.builder().build());
        ReflectionTestUtils.setField(publisher, "outboxPublisher", realOutbox);

        assertThatThrownBy(() -> publisher.publish("topic.test", "key-1", new Object()))
            .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void publishInsideActiveTransactionWritesOutboxRow() {
        OutboxPublisher realOutbox = new OutboxPublisher(jdbcTemplate, JsonMapper.builder().build());
        ReflectionTestUtils.setField(publisher, "outboxPublisher", realOutbox);
        TransactionSynchronizationManager.setActualTransactionActive(true);

        publisher.publish("topic.test", "key-1", new Object());

        verify(jdbcTemplate).update(anyString(), eq("topic.test"), eq("key-1"), anyString(), any());
    }
}
