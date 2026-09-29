package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.ops_ms.service.JobSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobEventListener {

    private final JobSettingService jobSettingService;

    @KafkaListener(topics = KafkaTopics.JOB_EVENTS, groupId = "ops.job-events")
    public void onJobExecution(JobExecutionEvent event) {
        if (event.getJobName() == null || event.getStatus() == null || event.getIdempotencyKey() == null) {
            throw new NonRetryableEventException("Job execution event without job name, status or idempotency key");
        }
        jobSettingService.record(event);
    }
}
