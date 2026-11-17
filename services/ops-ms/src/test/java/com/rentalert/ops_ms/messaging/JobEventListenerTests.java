package com.rentalert.ops_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobStatus;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.ops_ms.service.JobSettingService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JobEventListenerTests {

    private final JobSettingService jobSettingService = mock(JobSettingService.class);
    private final JobEventListener listener = createListener();

    private JobEventListener createListener() {
        JobEventListener created = new JobEventListener(jobSettingService);
        InboxGuard inboxGuard = mock(InboxGuard.class);
        Set<UUID> processed = new HashSet<>();
        when(inboxGuard.processOnce(any(), any(), any())).thenAnswer(invocation -> {
            if (!processed.add(invocation.<UUID>getArgument(1))) {
                return false;
            }
            invocation.<Runnable>getArgument(2).run();
            return true;
        });
        ReflectionTestUtils.setField(created, "inboxGuard", inboxGuard);
        return created;
    }

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
    void duplicateEventIsRecordedOnce() {
        JobExecutionEvent event = JobExecutionEvent.builder()
            .jobName(JobName.CURRENCY_RATE_SYNC)
            .status(JobStatus.SUCCESS)
            .build();

        listener.onJobExecution(event);
        listener.onJobExecution(event);

        verify(jobSettingService, times(1)).record(event);
    }

    @Test
    void eventWithoutStatusIsNotRetried() {
        JobExecutionEvent event = JobExecutionEvent.builder().jobName(JobName.CURRENCY_RATE_SYNC).build();

        assertThatThrownBy(() -> listener.onJobExecution(event))
            .isInstanceOf(NonRetryableEventException.class);
        verify(jobSettingService, never()).record(any());
    }
}
