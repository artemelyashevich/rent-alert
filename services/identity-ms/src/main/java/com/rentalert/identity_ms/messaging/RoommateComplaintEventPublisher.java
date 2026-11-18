package com.rentalert.identity_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.roommate.RoommateComplaintEvent;
import com.rentalert.dto.profile.FeedbackSubjectType;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RoommateComplaintEventPublisher extends AbstractOutboxEventPublisher {

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(FeedbackSubjectType subjectType, Long subjectId) {
        if (subjectType != FeedbackSubjectType.ROOMMATE_PROFILE && subjectType != FeedbackSubjectType.ROOMMATE_LISTING) {
            return;
        }
        RoommateComplaintEvent.SubjectType complaintSubject = RoommateComplaintEvent.SubjectType.valueOf(subjectType.name());
        RoommateComplaintEvent event = new RoommateComplaintEvent(UUID.randomUUID(), complaintSubject, subjectId);
        publish(KafkaTopics.ROOMMATE_COMPLAINTS, String.valueOf(subjectId), event);
    }
}
