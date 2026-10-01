package com.rentalert.processing_ms.service.impl;

import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.common.exception.AppNotFoundException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.repository.CityRepository;
import com.rentalert.processing_ms.service.CityService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private static final String NAMES_KEY = "names";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final CityRepository cityRepository;
    private final CacheService cacheService;

    @Override
    public Page<City> findAll(Pageable pageable) {
        return cityRepository.findAll(pageable);
    }

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

    @Override
    public City findById(Long id) {
        return cityRepository.findById(id)
            .orElseThrow(() -> new AppNotFoundException(InternalExceptionCode.EX404_CITY_NOT_FOUND));
    }

    @Override
    @Transactional
    public City create(ScheduleProvider provider, String name, String cityCode, boolean active) {
        if (cityRepository.existsByProviderAndCityCode(provider, cityCode)) {
            throw new AppBadRequestException(InternalExceptionCode.EX400_CITY_ALREADY_EXISTS);
        }
        City saved = cityRepository.save(City.builder()
            .provider(provider)
            .name(name)
            .cityCode(cityCode)
            .active(active)
            .build());
        evictNames();
        log.info("City created: id={}, provider={}, code={}, active={}", saved.getId(), provider, cityCode, active);
        return saved;
    }

    @Override
    @Transactional
    public City update(Long id, String name, boolean active) {
        City city = findById(id);
        city.setName(name);
        city.setActive(active);
        City updated = cityRepository.save(city);
        evictNames();
        log.info("City updated: id={}, active={}", id, active);
        return updated;
    }

    @Override
    @Transactional
    public City setActive(Long id, boolean active) {
        City city = findById(id);
        city.setActive(active);
        City updated = cityRepository.save(city);
        log.info("City active flag changed: id={}, active={}", id, active);
        return updated;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        cityRepository.delete(findById(id));
        evictNames();
        log.info("City deleted: id={}", id);
    }

    private void evictNames() {
        cacheService.evict(CacheType.CITY_NAMES, NAMES_KEY);
    }
}
