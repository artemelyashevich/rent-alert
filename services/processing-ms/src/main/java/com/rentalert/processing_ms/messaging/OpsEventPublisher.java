package com.rentalert.processing_ms.messaging;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends job runs to ops-ms through the transactional outbox.
 */
@Component
@RequiredArgsConstructor
public class OpsEventPublisher {

    private final OutboxPublisher outboxPublisher;

    @Transactional
    public void publishJobExecution(JobExecutionEvent event) {
        outboxPublisher.publish(KafkaTopics.JOB_EVENTS, event.getJobName().name(), event);
    }
}
