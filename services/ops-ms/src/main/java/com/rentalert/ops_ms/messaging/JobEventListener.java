package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.AbstractIdempotentKafkaListener;
import com.rentalert.ops_ms.service.JobSettingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobEventListener extends AbstractIdempotentKafkaListener {

    private static final String CONSUMER = "ops.job-events";

    private final JobSettingService jobSettingService;

    public JobEventListener(JobSettingService jobSettingService) {
        super(CONSUMER);
        this.jobSettingService = jobSettingService;
    }

    @KafkaListener(topics = KafkaTopics.JOB_EVENTS, groupId = CONSUMER)
    public void onJobExecution(JobExecutionEvent event) {
        if (event.getJobName() == null || event.getStatus() == null || event.getIdempotencyKey() == null) {
            throw new NonRetryableEventException("Job execution event without job name, status or idempotency key");
        }
        processOnce(event.getIdempotencyKey(), () -> jobSettingService.record(event));
    }
}
