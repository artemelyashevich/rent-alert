package com.rentalert.processing_ms.messaging;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.kafka.outbox.OutboxPublisher;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OpsEventPublisherTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final OpsEventPublisher publisher = new OpsEventPublisher(outboxPublisher);

    @Test
    void publishesJobEventKeyedByJobName() {
        JobExecutionEvent event = JobExecutionEvent.builder().jobName(JobName.CURRENCY_RATE_SYNC).build();

        publisher.publishJobExecution(event);

        verify(outboxPublisher).publish(KafkaTopics.JOB_EVENTS, "CURRENCY_RATE_SYNC", event);
    }
}
