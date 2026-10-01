package com.rentalert.processing_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.listing.SchedulerSettingsRequest;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.processing_ms.domain.SchedulerSettings;
import com.rentalert.processing_ms.mapper.SchedulerSettingsMapper;
import com.rentalert.processing_ms.repository.SchedulerSettingRepository;
import com.rentalert.processing_ms.service.SchedulerSettingsService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerSettingsServiceImpl implements SchedulerSettingsService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final SchedulerSettingRepository schedulerSettingRepository;
    private final SchedulerSettingsMapper schedulerSettingsMapper;
    private final CacheService cacheService;

    @Override
    @Transactional
    public SchedulerSettingsResponse create(SchedulerSettingsRequest request) {
        ScheduleProvider provider = ScheduleProvider.getScheduleProvider(request.getProviderName());
        if (schedulerSettingRepository.existsByProviderName(provider)) {
            String message = "Scheduler setting with provider name: %s already exists!".formatted(provider);
            log.warn(message);
            throw new AppException(message, HttpStatus.BAD_REQUEST);
        }
        SchedulerSettings saved = schedulerSettingRepository.save(schedulerSettingsMapper.toEntity(request));
        cacheService.evict(CacheType.SCHEDULER_SETTINGS, provider);
        log.info("New scheduler settings has been created: {}", provider);
        return schedulerSettingsMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SchedulerSettingsResponse update(SchedulerSettingsRequest request) {
        ScheduleProvider provider = ScheduleProvider.getScheduleProvider(request.getProviderName());
        SchedulerSettings settings = schedulerSettingRepository.findByProviderName(provider)
            .orElseThrow(() -> throwNotFoundException(provider));
        schedulerSettingsMapper.update(settings, request);
        SchedulerSettings updated = schedulerSettingRepository.save(settings);
        cacheService.evict(CacheType.SCHEDULER_SETTINGS, provider);
        log.info("Scheduler settings has been updated: {}", provider);
        return schedulerSettingsMapper.toResponse(updated);
    }

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

    @Override
    public List<SchedulerSettingsResponse> findAll() {
        return schedulerSettingsMapper.toResponse(schedulerSettingRepository.findAll());
    }

    @Override
    @Transactional
    public void delete(ScheduleProvider provider) {
        if (!schedulerSettingRepository.existsByProviderName(provider)) {
            throw throwNotFoundException(provider);
        }
        schedulerSettingRepository.deleteByProviderName(provider);
        cacheService.evict(CacheType.SCHEDULER_SETTINGS, provider);
        log.info("Scheduler settings has been deleted: {}", provider);
    }

    private AppException throwNotFoundException(ScheduleProvider scheduleProvider) {
        String message = "Scheduler settings with provider name: %s not found".formatted(scheduleProvider);
        log.warn(message);
        return new AppException(message, HttpStatus.NOT_FOUND);
    }
}
