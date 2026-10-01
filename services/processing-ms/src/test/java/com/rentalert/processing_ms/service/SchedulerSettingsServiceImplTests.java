package com.rentalert.processing_ms.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.listing.SchedulerSettingsRequest;
import com.rentalert.processing_ms.domain.SchedulerSettings;
import com.rentalert.processing_ms.mapper.SchedulerSettingsMapper;
import com.rentalert.processing_ms.repository.SchedulerSettingRepository;
import com.rentalert.processing_ms.service.impl.SchedulerSettingsServiceImpl;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SchedulerSettingsServiceImplTests {

    private final SchedulerSettingRepository repository = mock(SchedulerSettingRepository.class);
    private final SchedulerSettingsMapper mapper = mock(SchedulerSettingsMapper.class);
    private final CacheService cacheService = mock(CacheService.class);
    private final SchedulerSettingsService service = new SchedulerSettingsServiceImpl(repository, mapper, cacheService);

    private final SchedulerSettingsRequest request = SchedulerSettingsRequest.builder().providerName("KUFAR").build();

    @Test
    void duplicateProviderIsRejected() {
        when(repository.existsByProviderName(ScheduleProvider.KUFAR)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(AppException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateEvictsCachedSettings() {
        SchedulerSettings settings = new SchedulerSettings();
        when(repository.findByProviderName(ScheduleProvider.KUFAR)).thenReturn(Optional.of(settings));
        when(repository.save(settings)).thenReturn(settings);

        service.update(request);

        verify(mapper).update(settings, request);
        verify(cacheService).evict(CacheType.SCHEDULER_SETTINGS, ScheduleProvider.KUFAR);
    }

    @Test
    void deletingMissingProviderIsNotFound() {
        when(repository.existsByProviderName(ScheduleProvider.KUFAR)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(ScheduleProvider.KUFAR)).isInstanceOf(AppException.class);
        verify(repository, never()).deleteByProviderName(any());
    }

    @Test
    void deleteEvictsCachedSettings() {
        when(repository.existsByProviderName(ScheduleProvider.KUFAR)).thenReturn(true);

        service.delete(ScheduleProvider.KUFAR);

        verify(repository).deleteByProviderName(ScheduleProvider.KUFAR);
        verify(cacheService).evict(CacheType.SCHEDULER_SETTINGS, ScheduleProvider.KUFAR);
    }
}
