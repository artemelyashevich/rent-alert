package com.rentalert.identity_ms.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.roommate.RoommateComplaintEvent;
import com.rentalert.dto.profile.FeedbackSubjectType;
import com.rentalert.kafka.outbox.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class RoommateComplaintEventPublisherTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final RoommateComplaintEventPublisher publisher = createPublisher();

    private RoommateComplaintEventPublisher createPublisher() {
        RoommateComplaintEventPublisher created = new RoommateComplaintEventPublisher();
        ReflectionTestUtils.setField(created, "outboxPublisher", outboxPublisher);
        return created;
    }

    @Test
    void profileComplaintIsKeyedBySubjectId() {
        publisher.publish(FeedbackSubjectType.ROOMMATE_PROFILE, 7L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.ROOMMATE_COMPLAINTS), eq("7"), captor.capture());
        RoommateComplaintEvent event = (RoommateComplaintEvent) captor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.subjectType()).isEqualTo(RoommateComplaintEvent.SubjectType.ROOMMATE_PROFILE);
        assertThat(event.subjectId()).isEqualTo(7L);
    }

    @Test
    void listingComplaintGetsNewEventIdEachTime() {
        publisher.publish(FeedbackSubjectType.ROOMMATE_LISTING, 11L);
        publisher.publish(FeedbackSubjectType.ROOMMATE_LISTING, 11L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher, times(2)).publish(eq(KafkaTopics.ROOMMATE_COMPLAINTS), eq("11"), captor.capture());
        RoommateComplaintEvent first = (RoommateComplaintEvent) captor.getAllValues().get(0);
        RoommateComplaintEvent second = (RoommateComplaintEvent) captor.getAllValues().get(1);
        assertThat(first.subjectType()).isEqualTo(RoommateComplaintEvent.SubjectType.ROOMMATE_LISTING);
        assertThat(first.eventId()).isNotEqualTo(second.eventId());
    }

    @Test
    void appFeedbackPublishesNothing() {
        publisher.publish(FeedbackSubjectType.APP, null);

        verifyNoInteractions(outboxPublisher);
    }
}
