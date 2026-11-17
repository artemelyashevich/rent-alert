package com.rentalert.roommate_ms.messaging;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.roommate.RoommateComplaintEvent;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.roommate_ms.service.RoommateComplaintHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RoommateComplaintListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "roommate.complaints";

    private final RoommateComplaintHandler complaintHandler;

    public RoommateComplaintListener(RoommateComplaintHandler complaintHandler) {
        super(CONSUMER);
        this.complaintHandler = complaintHandler;
    }

    /**
     * Counts a complaint once per event id and hides or pauses the subject after the reports threshold. A complaint
     * without an event id or subject is not retryable and goes to the dead-letter topic.
     */
    @KafkaListener(topics = KafkaTopics.ROOMMATE_COMPLAINTS, groupId = CONSUMER)
    public void onComplaint(RoommateComplaintEvent event) {
        if (event.eventId() == null || event.subjectType() == null || event.subjectId() == null) {
            throw new NonRetryableEventException("Roommate complaint without eventId, subjectType or subjectId: " + event);
        }
        processOnce(event.eventId(),
            () -> complaintHandler.handle(event.subjectType(), event.subjectId()));
    }
}
