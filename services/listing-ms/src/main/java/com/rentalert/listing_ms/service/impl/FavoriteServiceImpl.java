package com.rentalert.listing_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.listing_ms.domain.Favorite;
import com.rentalert.listing_ms.domain.FavoriteEmbeddedId;
import com.rentalert.listing_ms.domain.Listing;
import com.rentalert.listing_ms.domain.ListingStatus;
import com.rentalert.listing_ms.domain.ListingStatusName;
import com.rentalert.listing_ms.dto.FavoriteCreateDto;
import com.rentalert.listing_ms.dto.FavoriteResponse;
import com.rentalert.listing_ms.dto.FavoriteUpdateStatusDto;
import com.rentalert.listing_ms.mapper.FavoriteMapper;
import com.rentalert.listing_ms.messaging.OpsEventPublisher;
import com.rentalert.listing_ms.repository.FavoriteRepository;
import com.rentalert.listing_ms.repository.FavoriteSpecification;
import com.rentalert.listing_ms.repository.ListingRepository;
import com.rentalert.listing_ms.service.FavoriteService;
import com.rentalert.listing_ms.service.ListingStatusService;
import com.rentalert.dto.event.user.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private static final int MAX_FAVORITES_PER_USER = 30;

    private static final Duration MAX_VIEWING_DATE_ADVANCE = Duration.ofDays(180);

    private final FavoriteRepository favoriteRepository;
    private final ListingRepository listingRepository;
    private final ListingStatusService listingStatusService;
    private final FavoriteMapper favoriteMapper;
    private final OpsEventPublisher opsEventPublisher;
    private final IdentityMsService identityMsService;

    @Override
    @Transactional
    public FavoriteResponse addToFavorites(Long userId, FavoriteCreateDto createDto) {
        Objects.requireNonNull(createDto, "createDto must not be null");

        log.debug("Adding listing {} to favorites for user {}", createDto.listingId(), userId);

        if (favoriteRepository.existsByIdUserIdAndIdListingId(userId, createDto.listingId())) {
            throw new AppException(InternalExceptionCode.EX400_FAVORITE_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        int currentCount = favoriteRepository.countByIdUserId(userId);
        if (currentCount >= MAX_FAVORITES_PER_USER) {
            throw new AppException(InternalExceptionCode.EX400_FAVORITE_LIMIT_REACHED, HttpStatus.BAD_REQUEST);
        }

        ensureUserExists(userId);

        Listing listing = listingRepository.findById(createDto.listingId())
                .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND));

        ListingStatus listingStatus = listingStatusService.findById(createDto.listingStatusId());
        Instant resolvedViewingDate = resolveViewingDate(listingStatus, createDto.viewingDate());

        Favorite favorite = Favorite.builder()
                .id(FavoriteEmbeddedId.builder()
                        .userId(userId)
                        .listingId(createDto.listingId())
                        .build())
                .listing(listing)
                .listingStatus(listingStatus)
                .viewingDate(resolvedViewingDate)
                .build();

        Favorite saved;
        try {
            saved = favoriteRepository.save(favorite);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition on addToFavorites detected: user={}, listing={}", userId, createDto.listingId());
            throw new AppException(InternalExceptionCode.EX400_FAVORITE_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        log.info("Listing {} added to favorites for user {}", createDto.listingId(), userId);
        opsEventPublisher.publishUserEvent(UserEvent.FAVORITE_ADDED, userId, createDto.listingId());

        return favoriteMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getUserFavorites(Long userId, ListingStatusName status, Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable must not be null");
        log.debug("Fetching favorites for user {} with pagination: page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());
        Specification<Favorite> spec = Specification.where(FavoriteSpecification.hasUserId(userId))
                .and(FavoriteSpecification.hasStatus(status));
        Page<Favorite> favorites = favoriteRepository.findAll(spec, pageable);
        log.info("Found {} favorites for user {} with status filter: {} on page {}",
                favorites.getNumberOfElements(), userId, status, pageable.getPageNumber());
        return favorites.map(favoriteMapper::toResponse);
    }

    @Override
    @Transactional
    public FavoriteResponse updateStatus(Long userId, Long listingId, FavoriteUpdateStatusDto updateDto) {
        Objects.requireNonNull(updateDto, "updateDto must not be null");
        log.debug("Updating status for favorite: user={}, listing={}", userId, listingId);

        FavoriteEmbeddedId favoriteEmbeddedId = FavoriteEmbeddedId.builder()
                .userId(userId)
                .listingId(listingId)
                .build();

        Favorite favorite = favoriteRepository.findById(favoriteEmbeddedId)
                .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_FAVORITE_NOT_FOUND, HttpStatus.NOT_FOUND));

        ListingStatus newStatus = listingStatusService.findById(updateDto.listingStatusId());

        Instant resolvedViewingDate = resolveViewingDate(newStatus, updateDto.viewingDate());

        favorite.setListingStatus(newStatus);
        favorite.setViewingDate(resolvedViewingDate);

        Favorite updated = favoriteRepository.save(favorite);
        log.info("Status updated for favorite: user={}, listing={}", userId, listingId);

        return favoriteMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void removeFromFavorites(Long userId, Long listingId) {
        log.debug("Removing listing {} from favorites for user {}", listingId, userId);

        FavoriteEmbeddedId favoriteEmbeddedId = FavoriteEmbeddedId.builder()
                .userId(userId)
                .listingId(listingId)
                .build();

        if (!favoriteRepository.existsById(favoriteEmbeddedId)) {
            throw new AppException(InternalExceptionCode.EX404_FAVORITE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        favoriteRepository.deleteById(favoriteEmbeddedId);
        log.info("Listing {} removed from favorites for user {}", listingId, userId);
        opsEventPublisher.publishUserEvent(UserEvent.FAVORITE_REMOVED, userId, listingId);
    }

    private void ensureUserExists(Long userId) {
        try {
            identityMsService.getProfile(userId);
        } catch (UpstreamHttpException e) {
            if (e.getUpstreamStatus() == HttpStatus.NOT_FOUND.value()) {
                throw new AppException(InternalExceptionCode.EX404_USER_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
            throw e;
        }
    }

    private Instant resolveViewingDate(ListingStatus status, Instant requestedDate) {
        boolean isCalendarStatus = status.isShowsInCalendar();
        if (isCalendarStatus && requestedDate == null) {
            log.warn("Validation failed: viewing date is required for calendar status {}", status.getName());
            throw new AppException(InternalExceptionCode.EX400_VIEWING_DATE_REQUIRED, HttpStatus.BAD_REQUEST);
        }
        if (!isCalendarStatus && requestedDate != null) {
            log.warn("Validation failed: viewing date is not applicable for status {}", status.getName());
            throw new AppException(InternalExceptionCode.EX400_VIEWING_DATE_NOT_APPLICABLE, HttpStatus.BAD_REQUEST);
        }
        if (requestedDate != null) {
            Instant now = Instant.now();
            if (requestedDate.isBefore(now)) {
                log.warn("Validation failed: viewing date {} is in the past", requestedDate);
                throw new AppException(InternalExceptionCode.EX400_VIEWING_DATE_IN_PAST, HttpStatus.BAD_REQUEST);
            }
            Instant maxAllowed = now.plus(MAX_VIEWING_DATE_ADVANCE);
            if (requestedDate.isAfter(maxAllowed)) {
                log.warn("Validation failed: viewing date {} is too far in the future (max {})", requestedDate, maxAllowed);
                throw new AppException(InternalExceptionCode.EX400_VIEWING_DATE_TOO_FAR_IN_FUTURE, HttpStatus.BAD_REQUEST);
            }
        }

        return isCalendarStatus ? requestedDate : null;
    }
}
