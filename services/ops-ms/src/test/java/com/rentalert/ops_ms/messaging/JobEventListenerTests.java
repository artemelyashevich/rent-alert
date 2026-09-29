package com.rentalert.ops_ms.messaging;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobStatus;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.ops_ms.service.JobSettingService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JobEventListenerTests {

    private final JobSettingService jobSettingService = mock(JobSettingService.class);
    private final JobEventListener listener = new JobEventListener(jobSettingService);

    @Test
    void recordsValidEvent() {
        JobExecutionEvent event = JobExecutionEvent.builder()
            .jobName(JobName.CURRENCY_RATE_SYNC)
            .status(JobStatus.SUCCESS)
            .build();

        listener.onJobExecution(event);

        verify(jobSettingService).record(event);
    }

    @Test
    void eventWithoutStatusIsNotRetried() {
        JobExecutionEvent event = JobExecutionEvent.builder().jobName(JobName.CURRENCY_RATE_SYNC).build();

        assertThatThrownBy(() -> listener.onJobExecution(event))
            .isInstanceOf(NonRetryableEventException.class);
        verify(jobSettingService, never()).record(any());
    }
}
