package com.rentalert.processing_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.processing_ms.domain.SchedulerSettings;
import com.rentalert.processing_ms.mapper.SchedulerSettingsMapper;
import com.rentalert.processing_ms.repository.SchedulerSettingRepository;
import com.rentalert.processing_ms.service.SchedulerSettingsService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerSettingsServiceImpl implements SchedulerSettingsService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final SchedulerSettingRepository schedulerSettingRepository;
    private final SchedulerSettingsMapper schedulerSettingsMapper;
    private final CacheService cacheService;

    @Override
    public SchedulerSettingsResponse findByProvider(ScheduleProvider scheduleProvider) {
        Optional<SchedulerSettingsResponse> cached = cacheService.get(CacheType.SCHEDULER_SETTINGS, scheduleProvider);
        if (cached.isPresent()) {
            return cached.get();
        }
        log.debug("Attempting find scheduler setting: {}", scheduleProvider);
        SchedulerSettings schedulerSettings = schedulerSettingRepository.findByProviderName(scheduleProvider)
            .orElseThrow(() -> throwNotFoundException(scheduleProvider));
        log.info("Scheduler settings has been found: {}", scheduleProvider);
        SchedulerSettingsResponse response = schedulerSettingsMapper.toResponse(schedulerSettings);
        cacheService.put(CacheType.SCHEDULER_SETTINGS, scheduleProvider, response, CACHE_TTL);
        return response;
    }

    private AppException throwNotFoundException(ScheduleProvider scheduleProvider) {
        String message = "Scheduler settings with provider name: %s not found".formatted(scheduleProvider);
        log.warn(message);
        return new AppException(message, HttpStatus.NOT_FOUND);
    }
}
