package com.rentalert.processing_ms.messaging;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.outbox.AbstractOutboxEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends job runs to ops-ms through the transactional outbox.
 */
@Component
public class OpsEventPublisher extends AbstractOutboxEventPublisher {

    @Transactional
    public void publishJobExecution(JobExecutionEvent event) {
        publish(KafkaTopics.JOB_EVENTS, event.getJobName().name(), event);
    }
}
