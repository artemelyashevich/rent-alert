package com.rentalert.ops_ms.service;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.dto.event.job.JobName;
import com.rentalert.dto.event.job.JobStatus;
import com.rentalert.ops_ms.domain.JobSetting;
import com.rentalert.ops_ms.mapper.JobSettingMapper;
import com.rentalert.ops_ms.repository.JobSettingRepository;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobSettingServiceImplTests {

    private final JobSettingRepository repository = mock(JobSettingRepository.class);
    private final JobSettingService service =
        new JobSettingServiceImpl(repository, Mappers.getMapper(JobSettingMapper.class));

    private final JobExecutionEvent event = JobExecutionEvent.builder()
        .jobName(JobName.CURRENCY_RATE_SYNC)
        .status(JobStatus.FAILED)
        .errorMessage("timeout")
        .affectedRecords(0L)
        .build();

    @Test
    void recordsNewEvent() {
        service.record(event);

        verify(repository).save(argThat((JobSetting job) -> event.getIdempotencyKey().equals(job.getEventId())
            && job.getJobName() == JobName.CURRENCY_RATE_SYNC
            && job.getStatus() == JobStatus.FAILED
            && "timeout".equals(job.getErrorMessage())));
    }

    @Test
    void ignoresRedeliveredEvent() {
        when(repository.existsByEventId(event.getIdempotencyKey())).thenReturn(true);

        service.record(event);

        verify(repository, never()).save(any());
    }
}
