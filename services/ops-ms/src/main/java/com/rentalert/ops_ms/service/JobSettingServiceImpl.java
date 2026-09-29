package com.rentalert.ops_ms.service;

import com.rentalert.dto.event.JobExecutionEvent;
import com.rentalert.ops_ms.mapper.JobSettingMapper;
import com.rentalert.ops_ms.repository.JobSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSettingServiceImpl implements JobSettingService {

    private final JobSettingRepository jobSettingRepository;
    private final JobSettingMapper jobSettingMapper;

    @Override
    @Transactional
    public void record(JobExecutionEvent event) {
        if (jobSettingRepository.existsByEventId(event.getIdempotencyKey())) {
            log.debug("Job event {} already recorded, skipping", event.getIdempotencyKey());
            return;
        }
        jobSettingRepository.save(jobSettingMapper.toEntity(event));
    }
}
