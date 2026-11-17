package com.rentalert.identity_ms.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.notification.NotificationRequest;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.kafka.outbox.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class RegistrationEventPublisherTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final RegistrationEventPublisher publisher = createPublisher();

    private RegistrationEventPublisher createPublisher() {
        RegistrationEventPublisher created = new RegistrationEventPublisher();
        ReflectionTestUtils.setField(created, "outboxPublisher", outboxPublisher);
        return created;
    }

    @Test
    void registrationNotifiesServicesAndAdmin() {
        UserProfile profile = new UserProfile();
        profile.setId(5L);

        publisher.publishRegistered(profile);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.USER_EVENTS), eq("5"), event.capture());
        assertThat(((UserActionEvent) event.getValue()).getEvent()).isEqualTo(UserEvent.USER_REGISTERED);
        assertThat(((UserActionEvent) event.getValue()).getInitiatorId()).isEqualTo(5L);

        ArgumentCaptor<Object> alert = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.notificationRequests(NotificationType.USER_CREATED)), eq("5"), alert.capture());
        NotificationRequest request = (NotificationRequest) alert.getValue();
        assertThat(request.type()).isEqualTo(NotificationType.USER_CREATED);
        assertThat(request.templateParams()).containsEntry("message", "5");
        assertThat(request.deduplicationKey()).isEqualTo("user-created:5");
    }
}
