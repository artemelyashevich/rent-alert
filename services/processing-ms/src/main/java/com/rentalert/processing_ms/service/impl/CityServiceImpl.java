package com.rentalert.processing_ms.service.impl;

import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.repository.CityRepository;
import com.rentalert.processing_ms.service.CityService;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private static final String NAMES_KEY = "names";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final CityRepository cityRepository;
    private final CacheService cacheService;

    @Override
    public List<City> findActiveByProvider(ScheduleProvider provider) {
        return cityRepository.findAllByProviderAndActiveTrue(provider);
    }

    @Override
    public List<String> findDistinctNames() {
        Optional<List<String>> cached = cacheService.get(CacheType.CITY_NAMES, NAMES_KEY);
        if (cached.isPresent()) {
            return cached.get();
        }
        List<String> names = cityRepository.findDistinctNames();
        cacheService.put(CacheType.CITY_NAMES, NAMES_KEY, names, CACHE_TTL);
        return names;
    }
}
