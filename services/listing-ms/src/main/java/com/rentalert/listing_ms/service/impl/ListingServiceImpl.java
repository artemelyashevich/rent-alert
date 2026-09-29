package com.rentalert.listing_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.exception.NoActiveFiltersException;
import com.rentalert.listing_ms.domain.Listing;
import com.rentalert.listing_ms.mapper.ListingEventMapper;
import com.rentalert.listing_ms.mapper.ListingMapper;
import com.rentalert.listing_ms.repository.ListingRepository;
import com.rentalert.listing_ms.repository.ListingSpecification;
import com.rentalert.listing_ms.service.CurrencyRateService;
import com.rentalert.listing_ms.service.ListingService;
import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final ListingEventMapper listingEventMapper;
    private final UserSessionService userSessionService;
    private final CurrencyRateService currencyRateService;

    @Override
    public Page<ListingDto> findAll(String filter, Set<String> sources, ListingCategory category, Pageable pageable) {
        UserSession currentUser = userSessionService.getCurrentUser();
        log.debug("Attempting findAll listings by user: {}, filters: sources={}, category={}", currentUser.getUsername(), sources, category);

        UserFilterResponse activeFilter = findActiveFilter(currentUser.getUserId());

        if (activeFilter == null) {
            log.warn("No active filters found for user: {}", currentUser.getUsername());
            throw new NoActiveFiltersException();
        }

        Set<ScheduleProvider> providers = null;

        if (sources != null && !sources.isEmpty()) {
            providers = sources.stream()
                    .map(ScheduleProvider::getScheduleProvider)
                    .collect(Collectors.toSet());
        }

        Specification<Listing> spec = Specification.where(ListingSpecification.hasSources(providers))
            .and(ListingSpecification.hasCategory(category))
            .and(ListingSpecification.matchesFilterWithCurrency(activeFilter, currencyRateService))
            .and(ListingSpecification.matchesKeyword(filter))
            .and(ListingSpecification.notHiddenByUser(currentUser.getUserId()));

        Page<ListingDto> result = listingRepository.findAll(spec, pageable).map(listingMapper::toDto);
        log.info("Found {} listings for user {}", result.getTotalElements(), currentUser.getUsername());
        return result;
    }

    // TODO: получать активный фильтр пользователя из identity-ms
    private UserFilterResponse findActiveFilter(Long userId) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Listing getById(Long id) {
        return listingRepository.findById(id)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findStaleListingIds(Instant cutoff, int limit) {
        return listingRepository.findStaleListingIds(cutoff, PageRequest.of(0, Math.max(1, limit)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(List<Long> listingIds) {
        if (listingIds.isEmpty()) {
            return 0;
        }
        int deleted = listingRepository.deleteByIdIn(listingIds);
        log.info("Deleted {} listing entries", deleted);
        return deleted;
    }

    @Override
    @Transactional
    public void saveCreated(ListingCreatedEvent event) {
        if (listingRepository.existsById(event.listingId())) {
            log.debug("Listing {} already stored, skipping", event.listingId());
            return;
        }
        listingRepository.save(listingEventMapper.toEntity(event));
        log.debug("Listing {} stored. source={}, sourceId={}", event.listingId(), event.source(), event.sourceId());
    }
}
