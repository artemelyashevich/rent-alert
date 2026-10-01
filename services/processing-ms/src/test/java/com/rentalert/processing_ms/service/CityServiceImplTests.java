package com.rentalert.processing_ms.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.repository.CityRepository;
import com.rentalert.processing_ms.service.impl.CityServiceImpl;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CityServiceImplTests {

    private final CityRepository repository = mock(CityRepository.class);
    private final CacheService cacheService = mock(CacheService.class);
    private final CityService service = new CityServiceImpl(repository, cacheService);

    @Test
    void duplicateCityIsRejected() {
        when(repository.existsByProviderAndCityCode(ScheduleProvider.KUFAR, "minsk")).thenReturn(true);

        assertThatThrownBy(() -> service.create(ScheduleProvider.KUFAR, "Минск", "minsk", true))
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(InternalExceptionCode.EX400_CITY_ALREADY_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createEvictsCityNames() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(ScheduleProvider.KUFAR, "Минск", "minsk", true);

        verify(cacheService).evict(CacheType.CITY_NAMES, "names");
    }

    @Test
    void deletingMissingCityIsNotFound() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(7L))
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(InternalExceptionCode.EX404_CITY_NOT_FOUND);
    }

    @Test
    void deleteEvictsCityNames() {
        City city = City.builder().name("Минск").build();
        when(repository.findById(7L)).thenReturn(Optional.of(city));

        service.delete(7L);

        verify(repository).delete(city);
        verify(cacheService).evict(CacheType.CITY_NAMES, "names");
    }
}
